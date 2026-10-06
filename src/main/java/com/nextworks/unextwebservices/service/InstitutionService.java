package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.agreement.AgreementResponseDTO;
import com.nextworks.unextwebservices.dto.agreement.AgreementUpdateRequestDTO;
import com.nextworks.unextwebservices.dto.dashboard.DashboardStatsResponseDTO;
import com.nextworks.unextwebservices.dto.directory.InstitutionDirectoryResponseDTO;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    // Numero minimo de instituciones que deben acreditar activamente a una empresa para que obtenga la insignia
    private static final int ACCREDITATION_THRESHOLD = 1;

    private final InstitutionProfileRepository institutionRepository;
    private final PostulantProfileRepository postulantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final InstitutionEndorsementRepository endorsementRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InternshipAgreementRepository agreementRepository;
    private final AcademicValidationRepository academicValidationRepository;

    // Listar alumnos pendientes de validación
    @Transactional(readOnly = true)
    public List<PendingStudentResponseDTO> getPendingStudents(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        PostulantProfile postulant = postulantRepository.findById(postulantId)
                .orElseThrow(() -> new RuntimeException("Postulante no encontrado"));

        if (postulant.getInstitutionProfile() == null ||
                !postulant.getInstitutionProfile().getId().equals(institution.getId())) {
            throw new RuntimeException("HTTP 403: Este estudiante no pertenece a tu institución.");
        }

        String notifTitle = "";
        String notifContent = "";
        String responseMessage = "";

        switch (status) {
            case APPROVED:
                postulant.setIsInstitutionVerified(true);

                notifTitle = "Perfil Académico Oficializado";
                notifContent = "Tu casa de estudios ha verificado oficialmente tu perfil. Ahora destacarás ante los reclutadores.";
                responseMessage = "El estudiante ha sido verificado exitosamente.";
                break;

            case REJECTED:
                postulant.setIsInstitutionVerified(false);
                postulant.setInstitutionProfile(null);

                notifTitle = "Asociación Académica Rechazada";
                notifContent = "La institución no ha podido verificar tus datos. Por favor, revisa tu información e intenta vincularte nuevamente.";
                responseMessage = "La solicitud del estudiante ha sido rechazada.";
                break;

            default:
                throw new RuntimeException("HTTP 400: Acción no permitida. El estado debe ser APPROVED o REJECTED.");
        }

        postulantRepository.save(postulant);

        if (!notifTitle.isEmpty()) {
            notificationService.createNotification(postulant.getUser(), notifTitle, notifContent);
        }

        return responseMessage;
    }

    // Otorgar Insignia a una Empresa: solo si la institucion tiene al menos un convenio APPROVED con ella
    @Transactional
    public String endorseCompany(String email, UUID recruiterId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

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
        if (request.getObservations() != null) {
            agreement.setObservations(request.getObservations());
        }
        agreementRepository.save(agreement);

        String notifTitle = "Actualización de Convenio de Prácticas";
        String notifContent = "La institución ha " + action + " el convenio: " + agreement.getTitle() +
                (request.getObservations() != null ? ". Observaciones: " + request.getObservations() : ".");

        notificationService.createNotification(postulantUser, notifTitle, notifContent);
        notificationService.createNotification(recruiterUser, notifTitle, notifContent);

        return mapAgreementToDTO(agreement);
    }

    private AgreementResponseDTO mapAgreementToDTO(InternshipAgreement a) {
        return AgreementResponseDTO.builder()
                .id(a.getId())
                .jobApplicationId(a.getJobApplication() != null ? a.getJobApplication().getId() : null)
                .title(a.getTitle())
                .companyName(a.getRecruiterProfile().getCompanyName())
                .studentName(a.getPostulantProfile().getFirstName() + " " + a.getPostulantProfile().getLastName())
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

    // Obtener Estadísticas del Dashboard
    @Transactional(readOnly = true)
    public DashboardStatsResponseDTO getDashboardStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        UUID instId = institution.getId();

        long verifiedStudents = postulantRepository.countByInstitutionProfileIdAndIsInstitutionVerifiedTrue(instId);
        long hiredStudents = postulantRepository.countHiredStudentsByInstitutionId(instId);
        long activeAgreements = agreementRepository.countByInstitutionProfileIdAndStatus(instId, AgreementStatus.APPROVED);
        long endorsedCompanies = endorsementRepository.countByInstitutionProfileIdAndRevokedAtIsNull(instId);
        double rate = 0.0;
        if (verifiedStudents > 0) {
            rate = ((double) hiredStudents / verifiedStudents) * 100;
        }

        return DashboardStatsResponseDTO.builder()
                .totalVerifiedStudents(verifiedStudents)
                .totalHiredStudents(hiredStudents)
                .totalActiveAgreements(activeAgreements)
                .totalEndorsedCompanies(endorsedCompanies)
                .employabilityRate(Math.round(rate * 100.0) / 100.0)
                .build();
    }

    // Directorio público de instituciones para el frontend
    @Transactional(readOnly = true)
    public List<InstitutionDirectoryResponseDTO> getAllInstitutions() {
        return institutionRepository.findAll().stream()
                .map(inst -> InstitutionDirectoryResponseDTO.builder()
                        .id(inst.getId())
                        .name(inst.getName())
                        .build())
                .toList();
    }

    // Listar validaciones de habilidades pendientes
    @Transactional(readOnly = true)
    public List<PendingValidationResponseDTO> getPendingSkillValidations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        List<AcademicValidation> pendingValidations = academicValidationRepository
                .findByInstitutionProfileIdAndStatus(institution.getId(), ValidationStatus.PENDING);

        return pendingValidations.stream()
                .map(v -> PendingValidationResponseDTO.builder()
                        .validationId(v.getId())
                        .studentName(v.getPostulantProfile().getFirstName() + " " + v.getPostulantProfile().getLastName())
                        .skillName(v.getTechnicalSkill().getName())
                        .evidenceUrl(v.getEvidenceUrl())
                        .status(v.getStatus().name())
                        .build())
                .toList();
    }

    // Aprobar o Rechazar la validación y notificar al alumno
    @Transactional
    public String updateSkillValidationStatus(String email, UUID validationId, ValidationUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        AcademicValidation validation = academicValidationRepository.findById(validationId)
                .orElseThrow(() -> new RuntimeException("Validación no encontrada"));

        if (!validation.getInstitutionProfile().getId().equals(institution.getId())) {
            throw new RuntimeException("No tienes permisos para modificar esta validación.");
        }

        validation.setStatus(ValidationStatus.valueOf(request.getStatus()));
        if (request.getObservations() != null) {
            validation.setObservation(request.getObservations());
        }

        academicValidationRepository.save(validation);

        String notifTitle = "Actualización de Habilidad";
        String notifContent = "Tu solicitud de validación académica para la habilidad '" +
                validation.getTechnicalSkill().getName() + "' ha sido " +
                (request.getStatus().equals("APPROVED") ? "APROBADA" : "RECHAZADA") + "." +
                " " + validation.getObservation();
        notificationService.createNotification(validation.getPostulantProfile().getUser(), notifTitle, notifContent);

        return "El estado de la validación se ha actualizado correctamente.";
    }
}
