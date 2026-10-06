package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.review.ReportRequestDTO;
import com.nextworks.unextwebservices.dto.review.ReportResponseDTO;
import com.nextworks.unextwebservices.dto.review.ReputationResponseDTO;
import com.nextworks.unextwebservices.dto.review.ReviewRequestDTO;
import com.nextworks.unextwebservices.dto.review.ReviewResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import com.nextworks.unextwebservices.entity.enums.ReportStatus;
import com.nextworks.unextwebservices.entity.enums.ReviewStatus;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    public static final String CONTRACT_NOT_FINISHED =
            "No se permite calificar si el contrato no ha finalizado";
    public static final String NO_REVIEWS = "Aún no tienes reseñas recibidas";
    public static final String NO_REVIEWS_FOR_STARS = "No hay reseñas con esa calificación";
    public static final String OWN_REVIEW = "No se permite reportar la propia reseña";
    public static final String WIDEN_REPUTATION =
            "No se encontraron candidatos con ese nivel de reputación. Amplía los filtros.";

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;
    private final InternshipAgreementRepository agreementRepository;
    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final PostulantProfileRepository postulantRepository;

    @Transactional
    public ReviewResponseDTO saveReview(String email, ReviewRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de reclutador no encontrado"));
        InternshipAgreement agreement = agreementRepository.findById(request.getAgreementId())
                .orElseThrow(() -> new RuntimeException("Convenio no encontrado"));

        if (!agreement.getRecruiterProfile().getId().equals(recruiter.getId())) {
            throw new RuntimeException("No puedes calificar un convenio que no te pertenece");
        }
        if (!isContractFinished(agreement)) {
            throw new RuntimeException(CONTRACT_NOT_FINISHED);
        }

        boolean publish = Boolean.TRUE.equals(request.getPublish());
        Review review = reviewRepository
                .findByInternshipAgreementIdAndRecruiterProfileId(agreement.getId(), recruiter.getId())
                .orElse(null);

        if (review != null && review.getStatus() == ReviewStatus.PUBLISHED) {
            throw new RuntimeException("Ya publicaste una reseña para este convenio");
        }
        if (review != null && review.getStatus() == ReviewStatus.HIDDEN) {
            throw new RuntimeException("Esta reseña está oculta por un reporte y no se puede volver a publicar");
        }

        if (review == null) {
            review = Review.builder()
                    .recruiterProfile(recruiter)
                    .postulantProfile(agreement.getPostulantProfile())
                    .internshipAgreement(agreement)
                    .build();
        }
        review.setStars(request.getStars());
        review.setComment(request.getComment().trim());
        review.setStatus(publish ? ReviewStatus.PUBLISHED : ReviewStatus.DRAFT);
        reviewRepository.save(review);
        return toDto(review);
    }

    @Transactional(readOnly = true)
    public ReputationResponseDTO getMyReputation(String email, Integer stars) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        List<Review> published = reviewRepository.findByPostulantProfileIdAndStatusOrderByCreatedAtDesc(
                postulant.getId(), ReviewStatus.PUBLISHED);
        if (published.isEmpty()) {
            throw new RuntimeException(NO_REVIEWS);
        }

        List<Review> visible = stars == null
                ? published
                : published.stream().filter(review -> review.getStars().equals(stars)).toList();
        if (stars != null && visible.isEmpty()) {
            throw new RuntimeException(NO_REVIEWS_FOR_STARS);
        }

        double average = published.stream().mapToInt(Review::getStars).average().orElse(0);
        return ReputationResponseDTO.builder()
                .averageStars(Math.round(average * 10.0) / 10.0)
                .totalReviews(published.size())
                .reviews(visible.stream().map(this::toDto).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public ReputationSnapshot snapshot(UUID postulantProfileId) {
        List<Review> published = reviewRepository.findByPostulantProfileIdAndStatusOrderByCreatedAtDesc(
                postulantProfileId, ReviewStatus.PUBLISHED);
        if (published.isEmpty()) {
            return new ReputationSnapshot(null, 0);
        }
        double average = published.stream().mapToInt(Review::getStars).average().orElse(0);
        return new ReputationSnapshot(Math.round(average * 10.0) / 10.0, published.size());
    }

    @Transactional
    public ReportResponseDTO reportReview(String email, UUID reviewId, ReportRequestDTO request) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));

        if (review.getStatus() == ReviewStatus.DRAFT) {
            throw new RuntimeException("No se puede reportar una reseña que aún es borrador");
        }
        if (review.getRecruiterProfile().getUser().getId().equals(reporter.getId())) {
            throw new RuntimeException(OWN_REVIEW);
        }

        ReviewReport report = reportRepository.save(ReviewReport.builder()
                .review(review)
                .reporter(reporter)
                .reason(request.getReason().trim())
                .status(ReportStatus.PENDING)
                .build());
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
        return ReportResponseDTO.builder()
                .id(report.getId())
                .reviewId(review.getId())
                .status(report.getStatus())
                .message("Reporte enviado. La reseña quedó oculta temporalmente.")
                .build();
    }

    @Transactional
    public String cancelReport(String email, UUID reportId) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        if (!report.getReporter().getId().equals(reporter.getId())) {
            throw new RuntimeException("No puedes cancelar un reporte que no enviaste");
        }
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new RuntimeException("Este reporte ya no se puede cancelar");
        }

        report.setStatus(ReportStatus.CANCELLED);
        reportRepository.save(report);

        Review review = report.getReview();
        if (!reportRepository.existsByReviewIdAndStatus(review.getId(), ReportStatus.PENDING)) {
            review.setStatus(ReviewStatus.PUBLISHED);
            reviewRepository.save(review);
        }
        return "Reporte cancelado.";
    }

    public boolean isContractFinished(InternshipAgreement agreement) {
        return agreement.getStatus() == AgreementStatus.APPROVED
                && agreement.getEndDate() != null
                && agreement.getEndDate().isBefore(LocalDate.now());
    }

    private ReviewResponseDTO toDto(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .agreementId(review.getInternshipAgreement().getId())
                .companyName(review.getRecruiterProfile().getCompanyName())
                .postulantName(review.getPostulantProfile().getFirstName() + " "
                        + review.getPostulantProfile().getLastName())
                .stars(review.getStars())
                .comment(review.getComment())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }

    public record ReputationSnapshot(Double averageStars, int totalReviews) {
    }
}
