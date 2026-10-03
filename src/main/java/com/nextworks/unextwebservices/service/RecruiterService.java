package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.ApplicationResponseDTO;
import com.nextworks.unextwebservices.dto.JobOfferRequestDTO;
import com.nextworks.unextwebservices.dto.JobOfferResponseDTO;
import com.nextworks.unextwebservices.entity.*;
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

    // Notificación automatica al momento de cambiar el estado de la vacante
    private final NotificationService notificationService;

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

        return mapToDTO(offer);
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
        return JobOfferResponseDTO.builder()
                .id(offer.getId())
                .companyName(offer.getRecruiterProfile().getCompanyName())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .requirements(offer.getRequirements())
                .location(offer.getLocation())
                .modality(offer.getModality())
                .experienceLevel(offer.getExperienceLevel())
                .minSalary(offer.getMinSalary())
                .maxSalary(offer.getMaxSalary())
                .createdAt(offer.getCreatedAt())
                .build();
    }

    // 1. Obtener todas las vacantes creadas por este reclutador
    @Transactional(readOnly = true)
    public List<JobOfferResponseDTO> getMyJobOffers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        return jobOfferRepository.findByRecruiterProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream().map(this::mapToDTO).toList();
    }

    // 2. Ver quiénes han postulado a una vacante específica
    @Transactional(readOnly = true)
    public List<ApplicationResponseDTO> getApplicationsForJob(String email, UUID jobId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
        JobOffer offer = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        if (!offer.getRecruiterProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("No tienes permiso para ver esta vacante");
        }

        return applicationRepository.findByJobOfferIdOrderByCreatedAtDesc(jobId)
                .stream().map(app -> ApplicationResponseDTO.builder()
                        .id(app.getId())
                        .jobTitle(offer.getTitle())
                        .companyName(profile.getCompanyName())
                        .status(app.getStatus())
                        .appliedAt(app.getCreatedAt())
                        .isSupervisedByInstitution(app.getIsSupervisedByInstitution())
                        .build())
                .toList();
    }

    // 3. Editar la vacante y notificar a los postulantes
    @Transactional
    public JobOfferResponseDTO updateJobOffer(String email, UUID jobId, JobOfferRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
        JobOffer offer = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Vacante no encontrada"));

        // Validar propiedad
        if (!offer.getRecruiterProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("No puedes editar una vacante que no te pertenece");
        }

        // Actualizar datos
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setRequirements(request.getRequirements());
        offer.setLocation(request.getLocation());
        offer.setModality(request.getModality());
        offer.setExperienceLevel(request.getExperienceLevel());
        offer.setMinSalary(request.getMinSalary());
        offer.setMaxSalary(request.getMaxSalary());

        jobOfferRepository.save(offer);

        // =========================================================
        // GATILLO AUTOMÁTICO EN CASCADA
        // =========================================================
        List<JobApplication> applications = applicationRepository.findByJobOfferIdOrderByCreatedAtDesc(jobId);

        for (JobApplication app : applications) {
            // Solo notificamos a los que siguen activos en el proceso
            if (app.getStatus() == ApplicationStatus.RECEIVED || app.getStatus() == ApplicationStatus.UNDER_REVIEW) {
                String notifTitle = "Actualización en la vacante: " + offer.getTitle();
                String notifContent = "La empresa " + profile.getCompanyName() + " ha modificado las condiciones, requisitos o rango salarial de la vacante a la que postulaste. Revisa los nuevos detalles.";

                notificationService.createNotification(app.getPostulantProfile().getUser(), notifTitle, notifContent);
            }
        }

        return mapToDTO(offer);
    }
}