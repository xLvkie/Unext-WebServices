package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.agreement.AgreementResponseDTO;
import com.nextworks.unextwebservices.dto.agreement.AgreementUpdateRequestDTO;
import com.nextworks.unextwebservices.dto.dashboard.DashboardStatsResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.DashboardSummaryResponseDTO;
import com.nextworks.unextwebservices.dto.validation.EndorsedCompanyResponseDTO;
import com.nextworks.unextwebservices.dto.validation.PendingStudentResponseDTO;
import com.nextworks.unextwebservices.dto.validation.PendingValidationResponseDTO;
import com.nextworks.unextwebservices.dto.validation.ValidationUpdateDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import com.nextworks.unextwebservices.entity.enums.ValidationStatus;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    // Numero minimo de instituciones que deben acreditar activamente a una empresa para que obtenga la insignia
    private static final int ACCREDITATION_THRESHOLD = 1;

    public static final String DASHBOARD_EMPTY = DashboardService.NO_DATA_MESSAGE;
    public static final String DASHBOARD_DB_ERROR = DashboardService.DB_ERROR_MESSAGE;

    private final InstitutionProfileRepository institutionRepository;
    private final PostulantProfileRepository postulantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final InstitutionEndorsementRepository endorsementRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InternshipAgreementRepository agreementRepository;
    private final AcademicValidationRepository academicValidationRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final DashboardService dashboardService;

    // Listar alumnos pendientes de validación
    @Transactional(readOnly = true)
    public List<PendingStudentResponseDTO> getPendingStudents(String email) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        List<PostulantProfile> pendingStudents = postulantRepository
                .findByInstitutionProfileIdAndIsInstitutionVerifiedFalse(institution.getId());

        return pendingStudents.stream()
                .map(p -> PendingStudentResponseDTO.builder()
                        .postulantId(p.getId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getUser().getEmail())
                        .isVerified(p.getIsInstitutionVerified())
                        .build())
                .toList();
    }

    // Aprobar o desaprobar el perfil del alumno
    @Transactional
    public String updateStudentAssociation(String email, UUID postulantId, ValidationStatus status) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        PostulantProfile postulant = postulantRepository.findById(postulantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Postulante no encontrado"));

        if (postulant.getInstitutionProfile() == null ||
                !postulant.getInstitutionProfile().getId().equals(institution.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este estudiante no pertenece a tu institución.");
        }

        String notifTitle;
        String notifContent;
        String responseMessage;

        switch (status) {
            case APPROVED -> {
                postulant.setIsInstitutionVerified(true);
                notifTitle = "Perfil Académico Oficializado";
                notifContent = "Tu casa de estudios ha verificado oficialmente tu perfil. Ahora destacarás ante los reclutadores.";
                responseMessage = "El estudiante ha sido verificado exitosamente.";
            }
            case REJECTED -> {
                postulant.setIsInstitutionVerified(false);
                postulant.setInstitutionProfile(null);
                notifTitle = "Asociación Académica Rechazada";
                notifContent = "La institución no ha podido verificar tus datos. Por favor, revisa tu información e intenta vincularte nuevamente.";
                responseMessage = "La solicitud del estudiante ha sido rechazada.";
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Acción no permitida. El estado debe ser APPROVED o REJECTED.");
        }

        postulantRepository.save(postulant);
        notificationService.createNotification(postulant.getUser(), notifTitle, notifContent);

        return responseMessage;
    }

    // Otorgar Insignia a una Empresa: solo si la institucion tiene al menos un convenio APPROVED con ella
    @Transactional
    public String endorseCompany(String email, UUID recruiterId) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        RecruiterProfile recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        if (!agreementRepository.existsByInstitutionProfileIdAndRecruiterProfileIdAndStatus(
                institution.getId(), recruiterId, AgreementStatus.APPROVED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo puedes acreditar empresas con convenios aprobados");
        }

        InstitutionEndorsement endorsement = endorsementRepository
                .findByInstitutionProfileIdAndRecruiterProfileId(institution.getId(), recruiterId)
                .orElse(null);

        if (endorsement != null && endorsement.getRevokedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta empresa ya cuenta con tu insignia de confianza.");
        }

        if (endorsement != null) {
            // Ya existia (estaba revocada): se reactiva en vez de crear una fila nueva
            endorsement.setRevokedAt(null);
            endorsement.setRevocationReason(null);
        } else {
            endorsement = InstitutionEndorsement.builder()
                    .institutionProfile(institution)
                    .recruiterProfile(recruiter)
                    .build();
        }

        endorsementRepository.save(endorsement);
        recalculateAccreditation(recruiter);

        String notifTitle = "¡Insignia de Empresa Aliada!";
        String notifContent = "Una institución educativa te ha otorgado su sello de confianza. Tus vacantes ahora destacarán para sus alumnos.";
        notificationService.createNotification(recruiter.getUser(), notifTitle, notifContent);

        return "Insignia otorgada exitosamente a " + recruiter.getCompanyName();
    }

    // Recalcula validationsCount (instituciones que acreditan activamente) e isValidated (umbral) de la empresa
    private void recalculateAccreditation(RecruiterProfile recruiter) {
        long activeCount = endorsementRepository.countByRecruiterProfileIdAndRevokedAtIsNull(recruiter.getId());
        recruiter.setValidationsCount((int) activeCount);
        recruiter.setIsValidated(activeCount >= ACCREDITATION_THRESHOLD);
        recruiterRepository.save(recruiter);
    }

    // Retirar Insignia a una Empresa (exige motivo, no borra la fila)
    @Transactional
    public String removeEndorsement(String email, UUID recruiterId, String reason) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        InstitutionEndorsement endorsement = endorsementRepository
                .findByInstitutionProfileIdAndRecruiterProfileId(institution.getId(), recruiterId)
                .filter(e -> e.getRevokedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa no cuenta con tu insignia."));

        endorsement.setRevokedAt(LocalDateTime.now());
        endorsement.setRevocationReason(reason);
        endorsementRepository.save(endorsement);

        RecruiterProfile recruiter = endorsement.getRecruiterProfile();
        recalculateAccreditation(recruiter);

        String notifTitle = "Insignia de Empresa Aliada retirada";
        String notifContent = "Una institución educativa ha retirado su sello de confianza de tu perfil.";
        notificationService.createNotification(recruiter.getUser(), notifTitle, notifContent);

        return "Insignia retirada exitosamente.";
    }

    // Listar Empresas Aliadas (solo acreditaciones activas)
    @Transactional(readOnly = true)
    public List<EndorsedCompanyResponseDTO> getEndorsedCompanies(String email) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        return endorsementRepository.findByInstitutionProfileIdAndRevokedAtIsNull(institution.getId())
                .stream()
                .map(e -> EndorsedCompanyResponseDTO.builder()
                        .recruiterId(e.getRecruiterProfile().getId())
                        .companyName(e.getRecruiterProfile().getCompanyName())
                        .endorsedAt(e.getCreatedAt())
                        .build())
                .toList();
    }

    // Listar Convenios de Prácticas
    @Transactional(readOnly = true)
    public List<AgreementResponseDTO> getAgreements(String email, AgreementStatus status) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        List<InternshipAgreement> agreements;
        if (status != null) {
            agreements = agreementRepository.findByInstitutionProfileIdAndStatusOrderByCreatedAtDesc(institution.getId(), status);
        } else {
            agreements = agreementRepository.findByInstitutionProfileIdOrderByCreatedAtDesc(institution.getId());
        }

        return agreements.stream().map(this::mapAgreementToDTO).toList();
    }

    // Actualizar Estado del Convenio
    @Transactional
    public AgreementResponseDTO updateAgreementStatus(String email, UUID agreementId, AgreementUpdateRequestDTO request) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        // Si el convenio es de otra institución, no se revela que existe (404)
        InternshipAgreement agreement = agreementRepository.findByIdAndInstitutionProfileId(agreementId, institution.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convenio no encontrado"));

        if (agreement.getStatus() != AgreementStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El convenio ya fue revisado");
        }

        if (request.getStatus() != AgreementStatus.APPROVED && request.getStatus() != AgreementStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado de la evaluación solo puede ser APPROVED o REJECTED.");
        }

        if (request.getStatus() == AgreementStatus.REJECTED &&
                (request.getObservations() == null || request.getObservations().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar un motivo para el rechazo");
        }

        User postulantUser = agreement.getPostulantProfile().getUser();
        User recruiterUser = agreement.getRecruiterProfile().getUser();
        String action = request.getStatus() == AgreementStatus.APPROVED ? "APROBADO" : "RECHAZADO";

        agreement.setStatus(request.getStatus());
        agreement.setReviewedAt(LocalDateTime.now());
        if (request.getObservations() != null && !request.getObservations().isBlank()) {
            agreement.setObservations(request.getObservations().trim());
        }
        agreementRepository.save(agreement);

        String notifTitle = "Actualización de Convenio de Prácticas";
        String notifContent = "La institución ha " + action + " el convenio: " + agreement.getTitle()
                + (agreement.getObservations() != null && !agreement.getObservations().isBlank()
                ? ". Observaciones: " + agreement.getObservations()
                : ".");

        notificationService.createNotification(postulantUser, notifTitle, notifContent);
        notificationService.createNotification(recruiterUser, notifTitle, notifContent);

        return mapAgreementToDTO(agreement);
    }

    public AgreementResponseDTO mapAgreementToDTO(InternshipAgreement a) {
        return AgreementResponseDTO.builder()
                .id(a.getId())
                .jobApplicationId(a.getJobApplication() != null ? a.getJobApplication().getId() : null)
                .title(a.getTitle())
                .companyName(a.getRecruiterProfile().getCompanyName())
                .studentName((a.getPostulantProfile().getFirstName() + " " + a.getPostulantProfile().getLastName()).trim())
                .startDate(a.getStartDate())
                .endDate(a.getEndDate())
                .weeklyHours(a.getWeeklyHours())
                .documentUrl(a.getDocumentUrl())
                .status(a.getStatus())
                .observations(a.getObservations())
                .reviewedAt(a.getReviewedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }

    // Obtener Estadísticas del Dashboard (reutiliza cálculo consolidado de DashboardService)
    @Transactional(readOnly = true)
    public DashboardStatsResponseDTO getDashboardStats(String email, String career) {
        DashboardSummaryResponseDTO summary = dashboardService.getSummary(email, career);
        if (summary.getMessage() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, summary.getMessage());
        }

        return DashboardStatsResponseDTO.builder()
                .totalVerifiedStudents(summary.getTotalVerifiedStudents())
                .totalHiredStudents(summary.getTotalHiredStudents())
                .totalActiveAgreements(summary.getTotalActiveAgreements())
                .totalEndorsedCompanies(summary.getTotalEndorsedCompanies())
                .employabilityRate(summary.getEmployabilityRate())
                .career(summary.getCareer())
                .build();
    }

    // Listar validaciones de habilidades pendientes
    @Transactional(readOnly = true)
    public List<PendingValidationResponseDTO> getPendingSkillValidations(String email) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        List<AcademicValidation> pendingValidations = academicValidationRepository
                .findByInstitutionProfileIdAndStatus(institution.getId(), ValidationStatus.PENDING);

        return pendingValidations.stream()
                .map(v -> PendingValidationResponseDTO.builder()
                        .validationId(v.getId())
                        .studentName((v.getPostulantProfile().getFirstName() + " " + v.getPostulantProfile().getLastName()).trim())
                        .skillName(v.getTechnicalSkill().getName())
                        .evidenceUrl(v.getEvidenceUrl())
                        .status(v.getStatus().name())
                        .build())
                .toList();
    }

    // Aprobar o Rechazar la validación y notificar al alumno
    @Transactional
    public String updateSkillValidationStatus(String email, UUID validationId, ValidationUpdateDTO request) {
        InstitutionProfile institution = findInstitutionByEmail(email);

        AcademicValidation validation = academicValidationRepository.findById(validationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Validación no encontrada"));

        if (!validation.getInstitutionProfile().getId().equals(institution.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para modificar esta validación.");
        }

        ValidationStatus newStatus;
        try {
            if (request.getStatus() == null || request.getStatus().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
            }
            newStatus = ValidationStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estado inválido. El estado debe ser APPROVED o REJECTED.");
        }

        if (newStatus != ValidationStatus.APPROVED && newStatus != ValidationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Acción no permitida. El estado debe ser APPROVED o REJECTED.");
        }

        validation.setStatus(newStatus);
        if (request.getObservations() != null) {
            validation.setObservation(request.getObservations());
        }
        academicValidationRepository.save(validation);

        // ACTUALIZAR LA HABILIDAD TÉCNICA DEL ESTUDIANTE (StudentSkill)
        StudentSkill skill = validation.getTechnicalSkill();
        if (skill != null) {
            if (newStatus == ValidationStatus.APPROVED) {
                skill.setIsValidatedByInstitution(true);
                skill.setValidatedInstitutionName(institution.getName());
                skill.setValidatedInstitutionLogoUrl(institution.getLogoUrl());
            } else {
                skill.setIsValidatedByInstitution(false);
                skill.setValidatedInstitutionName(null);
                skill.setValidatedInstitutionLogoUrl(null);
            }
            studentSkillRepository.save(skill);
        }

        String obsText = (validation.getObservation() != null && !validation.getObservation().isBlank())
                ? " Observaciones: " + validation.getObservation()
                : "";

        String skillName = skill != null ? skill.getName() : "Habilidad";
        String notifTitle = "Actualización de Habilidad";
        String notifContent = "Tu solicitud de validación académica para la habilidad '" +
                skillName + "' ha sido " +
                (newStatus == ValidationStatus.APPROVED ? "APROBADA" : "RECHAZADA") + "." + obsText;
        notificationService.createNotification(validation.getPostulantProfile().getUser(), notifTitle, notifContent);

        return "El estado de la validación se ha actualizado correctamente.";
    }

    private InstitutionProfile findInstitutionByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de institución no encontrado"));
    }
}
