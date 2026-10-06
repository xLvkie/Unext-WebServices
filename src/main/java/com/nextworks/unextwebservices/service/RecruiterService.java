package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.agreement.AgreementCreateRequestDTO;
import com.nextworks.unextwebservices.dto.agreement.AgreementResponseDTO;
import com.nextworks.unextwebservices.dto.directory.RecruiterDirectoryResponseDTO;
import com.nextworks.unextwebservices.dto.job.JobOfferRequestDTO;
import com.nextworks.unextwebservices.dto.job.JobOfferResponseDTO;
import com.nextworks.unextwebservices.dto.job.JobOfferUpdateDTO;
import com.nextworks.unextwebservices.dto.job.RecruiterApplicationResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import com.nextworks.unextwebservices.entity.enums.ApplicationStatus;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final PostulantProfileRepository postulantRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final InternshipAgreementRepository internshipAgreementRepository;
    private final NotificationService notificationService;
    private final MatchingService matchingService;
    private final ReviewService reviewService;

    /* ====================================
    // Creación de nueva vacante
    // ==================================== */
    @Transactional
    public JobOfferResponseDTO createJobOffer(String email, JobOfferRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de reclutador no encontrado"));

        JobOffer offer = JobOffer.builder()
                .recruiterProfile(profile)
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .location(request.getLocation())
                .modality(request.getModality())
                .experienceLevel(request.getExperienceLevel())
                .minSalary(request.getMinSalary())
                .maxSalary(request.getMaxSalary())
                .isActive(true)
                .build();

        jobOfferRepository.save(offer);
        matchingService.replaceRequiredSkills(offer, request.getRequiredSkills());
        jobOfferRepository.save(offer);
        matchingService.notifyCompatiblePostulants(offer);

        return matchingService.toOfferDto(offer);
    }

    /* ====================================
    // Cambiar estado de vacante
    // ==================================== */
    @Transactional
    public String updateApplicationStatus(String email, UUID applicationId, ApplicationStatus newStatus) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de reclutador no encontrado"));

        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Postulación no encontrada"));

        if (!application.getJobOffer().getRecruiterProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("No tienes permiso para modificar esta postulación");
        }

        application.setStatus(newStatus);
        applicationRepository.save(application);

        User postulantUser = application.getPostulantProfile().getUser();
        String jobTitle = application.getJobOffer().getTitle();

        String notifTitle = "";
        String notifContent = "";

        switch (newStatus) {
            case UNDER_REVIEW:
                notifTitle = "¡Avanzaste en el proceso!";
                notifContent = "Tu postulación para '" + jobTitle + "' está siendo revisada. ¡El chat ha sido habilitado!";
                break;
            case ACCEPTED:
                notifTitle = "¡Felicidades, fuiste aceptado!";
                notifContent = "Has sido seleccionado para la vacante de '" + jobTitle + "'.";
                break;
            case REJECTED:
                notifTitle = "Actualización de tu postulación";
                notifContent = "El proceso para '" + jobTitle + "' ha concluido. No fuiste seleccionado esta vez.";
                break;
            default:
                break;
        }

        if (!notifTitle.isEmpty()) {
            notificationService.createNotification(postulantUser, notifTitle, notifContent);
        }

        return "Estado actualizado a " + newStatus.name() + " exitosamente.";
    }

    private JobOfferResponseDTO mapToDTO(JobOffer offer) {
        return matchingService.toOfferDto(offer);
    }

    /* ============================================
    // Obtener, Mas info, Editar vacante de empresa
    // ============================================ */
    @Transactional(readOnly = true)
    public List<JobOfferResponseDTO> getMyJobOffers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        return jobOfferRepository.findByRecruiterProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<RecruiterApplicationResponseDTO> getApplicationsForJob(
            String email,
            UUID jobId,
            String career,
            Integer minCycle,
            String skill,
            Double minRating,
            Integer minReviews) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
        JobOffer offer = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        if (!offer.getRecruiterProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("No tienes permiso para ver esta vacante");
        }

        List<String> requiredSkills = matchingService.requiredSkillNames(offer);
        String careerFilter = career == null ? null : career.trim();
        String skillFilter = skill == null ? null : skill.trim();
        boolean hasAcademicFilters = (careerFilter != null && !careerFilter.isEmpty())
                || minCycle != null
                || (skillFilter != null && !skillFilter.isEmpty());
        boolean hasReputationFilters = minRating != null || minReviews != null;

        List<RecruiterApplicationResponseDTO> results = applicationRepository
                .findByJobOfferIdOrderByCreatedAtDesc(jobId)
                .stream()
                .map(app -> {
                    PostulantProfile postulant = app.getPostulantProfile();
                    List<String> skills = matchingService.studentSkillNames(postulant);
                    MatchingService.MatchResult match = matchingService.evaluate(skills, requiredSkills);
                    ReviewService.ReputationSnapshot reputation = reviewService.snapshot(postulant.getId());
                    return RecruiterApplicationResponseDTO.builder()
                            .id(app.getId())
                            .postulantProfileId(postulant.getId())
                            .firstName(postulant.getFirstName())
                            .lastName(postulant.getLastName())
                            .career(postulant.getCareer())
                            .currentCycle(postulant.getCurrentCycle())
                            .headline(postulant.getHeadline())
                            .isInstitutionVerified(postulant.getIsInstitutionVerified())
                            .skills(skills)
                            .status(app.getStatus())
                            .appliedAt(app.getCreatedAt())
                            .compatibilityPercent(match.percent())
                            .compatibilityLabel(match.label())
                            .matchingHint(match.hint())
                            .averageStars(reputation.averageStars())
                            .reviewCount(reputation.totalReviews())
                            .build();
                })
                .filter(dto -> matchesFilters(dto, careerFilter, minCycle, skillFilter, minRating, minReviews))
                .toList();

        if (results.isEmpty() && (hasAcademicFilters || hasReputationFilters)) {
            if (hasReputationFilters && !hasAcademicFilters) {
                throw new RuntimeException(ReviewService.WIDEN_REPUTATION);
            }
            if (hasReputationFilters) {
                throw new RuntimeException(
                        "No se encontraron candidatos que coincidan con estos criterios académicos y de reputación. Amplía los filtros.");
            }
            throw new RuntimeException(MatchingService.NO_CANDIDATES_MESSAGE);
        }
        return results;
    }

    private boolean matchesFilters(
            RecruiterApplicationResponseDTO dto,
            String career,
            Integer minCycle,
            String skill,
            Double minRating,
            Integer minReviews) {
        if (career != null && !career.isEmpty()) {
            if (dto.getCareer() == null
                    || !dto.getCareer().toLowerCase().contains(career.toLowerCase())) {
                return false;
            }
        }
        if (minCycle != null) {
            if (dto.getCurrentCycle() == null || dto.getCurrentCycle() < minCycle) {
                return false;
            }
        }
        if (skill != null && !skill.isEmpty()) {
            boolean hasSkill = dto.getSkills() != null && dto.getSkills().stream()
                    .anyMatch(name -> name.equalsIgnoreCase(skill));
            if (!hasSkill) {
                return false;
            }
        }
        if (minRating != null) {
            if (dto.getAverageStars() == null || dto.getAverageStars() < minRating) {
                return false;
            }
        }
        if (minReviews != null) {
            if (dto.getReviewCount() == null || dto.getReviewCount() < minReviews) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public JobOfferResponseDTO updateJobOffer(String email, UUID jobId, JobOfferUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
        JobOffer offer = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        if (!offer.getRecruiterProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("No puedes editar una vacante que no te pertenece");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            offer.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            offer.setDescription(request.getDescription());
        }
        if (request.getRequirements() != null && !request.getRequirements().isBlank()) {
            offer.setRequirements(request.getRequirements());
        }
        if (request.getLocation() != null) {
            offer.setLocation(request.getLocation());
        }
        if (request.getModality() != null) {
            offer.setModality(request.getModality());
        }
        if (request.getExperienceLevel() != null) {
            offer.setExperienceLevel(request.getExperienceLevel());
        }
        if (request.getMinSalary() != null) {
            offer.setMinSalary(request.getMinSalary());
        }
        if (request.getMaxSalary() != null) {
            offer.setMaxSalary(request.getMaxSalary());
        }
        if (request.getRequiredSkills() != null) {
            matchingService.replaceRequiredSkills(offer, request.getRequiredSkills());
        }

        jobOfferRepository.save(offer);


        List<JobApplication> applications = applicationRepository.findByJobOfferIdOrderByCreatedAtDesc(jobId);
        for (JobApplication app : applications) {
            if (app.getStatus() == ApplicationStatus.RECEIVED || app.getStatus() == ApplicationStatus.UNDER_REVIEW) {
                String notifTitle = "Actualización en la vacante: " + offer.getTitle();
                String notifContent = "La empresa " + profile.getCompanyName() + " ha modificado las condiciones, requisitos o rango salarial de la vacante a la que postulaste. Revisa los nuevos detalles.";
                notificationService.createNotification(app.getPostulantProfile().getUser(), notifTitle, notifContent);
            }
        }

        return mapToDTO(offer);
    }

    // LISTADO DE TODAS LAS EMPRESAS
    @Transactional(readOnly = true)
    public List<RecruiterDirectoryResponseDTO> getAllRecruiters() {
        return recruiterRepository.findAll().stream()
                .map(r -> RecruiterDirectoryResponseDTO.builder()
                        .id(r.getId())
                        .userId(r.getUser().getId())
                        .companyName(r.getCompanyName())
                        .build())
                .toList();
    }

    /* ==============================================
    // Creación de convenio reclutador - insti - post
    // ============================================== */
    @Transactional
    public AgreementResponseDTO createAgreement(String email, AgreementCreateRequestDTO request) {
        // Identificar a la Empresa que hace la solicitud
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de reclutador no encontrado"));

        // Identificar al Alumno
        PostulantProfile postulant = postulantRepository.findById(request.getPostulantId())
                .orElseThrow(() -> new RuntimeException("Postulante no encontrado"));

        // Validar que el alumno tenga el respaldo de una Institución
        if (postulant.getInstitutionProfile() == null || !postulant.getIsInstitutionVerified()) {
            throw new RuntimeException("HTTP 400: No se puede crear un convenio institucional porque el estudiante no está verificado por ninguna universidad.");
        }

        InstitutionProfile institution = postulant.getInstitutionProfile();
        InternshipAgreement agreement = InternshipAgreement.builder()
                .title(request.getTitle())
                .recruiterProfile(recruiter)
                .postulantProfile(postulant)
                .institutionProfile(institution)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .weeklyHours(request.getWeeklyHours())
                .documentUrl(request.getDocumentUrl())
                .observations(request.getObservations())
                .status(AgreementStatus.PENDING)
                .build();

        internshipAgreementRepository.save(agreement);

        String notifTitle = "Nuevo Convenio de Prácticas: " + request.getTitle();
        String notifContent = "La empresa " + recruiter.getCompanyName() +
                " ha registrado un convenio para tu alumno " + postulant.getFirstName() +
                ". Por favor, revisa el documento y aprueba la solicitud.";
        notificationService.createNotification(institution.getUser(), notifTitle, notifContent);

        return AgreementResponseDTO.builder()
                .id(agreement.getId())
                .title(agreement.getTitle())
                .companyName(recruiter.getCompanyName())
                .studentName(postulant.getFirstName() + " " + postulant.getLastName())
                .startDate(agreement.getStartDate())
                .endDate(agreement.getEndDate())
                .weeklyHours(agreement.getWeeklyHours())
                .documentUrl(agreement.getDocumentUrl())
                .status(agreement.getStatus())
                .observations(agreement.getObservations())
                .createdAt(agreement.getCreatedAt()) // Asegúrate de que el @PrePersist de tu entidad le asigne valor
                .build();
    }
}