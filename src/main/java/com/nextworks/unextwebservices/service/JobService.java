package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.job.ApplicationResponseDTO;
import com.nextworks.unextwebservices.dto.job.JobOfferResponseDTO;
import com.nextworks.unextwebservices.dto.job.JobRecommendationResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.JobOfferRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final NotificationService notificationService;
    private final MatchingService matchingService;

    /* ============================================
    // Buscar, aplicar y obtener mis postulaciones
    // ============================================ */
    @Transactional(readOnly = true)
    public List<JobOfferResponseDTO> searchJobs(String keyword, JobModality modality, ExperienceLevel experience) {
        String searchKeyword = (keyword == null) ? "" : keyword;
        List<JobOffer> offers = jobOfferRepository.searchActiveOffers(searchKeyword, modality, experience);

        return offers.stream()
                .map(matchingService::toOfferDto)
                .toList();
    }

    @Transactional
    public ApplicationResponseDTO applyToJob(String email, UUID jobId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        JobOffer offer = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        if (!offer.getIsActive()) {
            throw new RuntimeException("Esta vacante ya no acepta postulaciones");
        }

        if (applicationRepository.existsByJobOfferIdAndPostulantProfileId(offer.getId(), profile.getId())) {
            throw new RuntimeException("Ya has postulado a esta vacante anteriormente");
        }

        JobApplication application = JobApplication.builder()
                .jobOffer(offer)
                .postulantProfile(profile)
                .build();

        applicationRepository.save(application);

        User recruiterUser = offer.getRecruiterProfile().getUser();
        String notifTitle = "Nueva postulación recibida";
        String notifContent = "Tienes un nuevo candidato para la vacante: " + offer.getTitle();
        notificationService.createNotification(recruiterUser, notifTitle, notifContent);

        return ApplicationResponseDTO.builder()
                .id(application.getId())
                .jobTitle(offer.getTitle())
                .companyName(offer.getRecruiterProfile().getCompanyName())
                .status(application.getStatus())
                .appliedAt(application.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDTO> getMyApplications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        List<JobApplication> applications = applicationRepository.findByPostulantProfileId(profile.getId());

        return applications.stream()
                .map(application -> ApplicationResponseDTO.builder()
                        .id(application.getId()) // ¡Este es el ID de la postulación que el chat necesita!
                        .jobTitle(application.getJobOffer().getTitle())
                        .companyName(application.getJobOffer().getRecruiterProfile().getCompanyName())
                        .status(application.getStatus())
                        .appliedAt(application.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> suggestSkills(String query) {
        return matchingService.suggestSkills(query);
    }

    @Transactional(readOnly = true)
    public List<JobRecommendationResponseDTO> getRecommendations(String email) {
        return matchingService.getRecommendations(email);
    }

    @Transactional
    public String dismissRecommendation(String email, UUID jobId) {
        return matchingService.dismissRecommendation(email, jobId);
    }
}