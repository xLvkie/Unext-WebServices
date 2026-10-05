package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.*;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionService {

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

    // Aprobar perfil de alumno y disparar gatillo
    @Transactional
    public String verifyStudentProfile(String email, UUID postulantId) {
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

        postulant.setIsInstitutionVerified(true);
        postulantRepository.save(postulant);

        String notifTitle = "Perfil Académico Oficializado";
        String notifContent = "Tu casa de estudios ha verificado oficialmente tu perfil. Ahora destacarás ante los reclutadores.";
        notificationService.createNotification(postulant.getUser(), notifTitle, notifContent);

        return "El estudiante ha sido verificado exitosamente.";
    }

    // Otorgar Insignia a una Empresa
    @Transactional
    public String endorseCompany(String email, UUID recruiterId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        RecruiterProfile recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        if (endorsementRepository.existsByInstitutionProfileIdAndRecruiterProfileId(institution.getId(), recruiterId)) {
            throw new RuntimeException("Esta empresa ya cuenta con tu insignia de confianza.");
        }

        InstitutionEndorsement endorsement = InstitutionEndorsement.builder()
                .institutionProfile(institution)
                .recruiterProfile(recruiter)
                .build();

        endorsementRepository.save(endorsement);

        String notifTitle = "¡Insignia de Empresa Aliada!";
        String notifContent = "Una institución educativa te ha otorgado su sello de confianza. Tus vacantes ahora destacarán para sus alumnos.";
        notificationService.createNotification(recruiter.getUser(), notifTitle, notifContent);

        return "Insignia otorgada exitosamente a " + recruiter.getCompanyName();
    }

    // Retirar Insignia a una Empresa
    @Transactional
    public String removeEndorsement(String email, UUID recruiterId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        InstitutionEndorsement endorsement = endorsementRepository
                .findByInstitutionProfileIdAndRecruiterProfileId(institution.getId(), recruiterId)
                .orElseThrow(() -> new RuntimeException("La empresa no cuenta con tu insignia."));

        endorsementRepository.delete(endorsement);
        return "Insignia retirada exitosamente.";
    }

    // Listar Empresas Aliadas
    @Transactional(readOnly = true)
    public List<EndorsedCompanyResponseDTO> getEndorsedCompanies(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        return endorsementRepository.findByInstitutionProfileId(institution.getId())
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

        return agreements.stream()
                .map(a -> AgreementResponseDTO.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .companyName(a.getRecruiterProfile().getCompanyName())
                        .studentName(a.getPostulantProfile().getFirstName() + " " + a.getPostulantProfile().getLastName())
                        .startDate(a.getStartDate())
                        .endDate(a.getEndDate())
                        .weeklyHours(a.getWeeklyHours())
                        .documentUrl(a.getDocumentUrl())
                        .status(a.getStatus())
                        .observations(a.getObservations())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();
    }

    // Actualizar Estado del Convenio (Aprobar/Rechazar) y Notificar
    @Transactional
    public AgreementResponseDTO updateAgreementStatus(String email, UUID agreementId, AgreementUpdateRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        InternshipAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Convenio no encontrado"));

        if (!agreement.getInstitutionProfile().getId().equals(institution.getId())) {
            throw new RuntimeException("No tienes permiso para modificar este convenio.");
        }

        agreement.setStatus(request.getStatus());
        if (request.getObservations() != null) {
            agreement.setObservations(request.getObservations());
        }

        agreementRepository.save(agreement);

        User postulantUser = agreement.getPostulantProfile().getUser();
        User recruiterUser = agreement.getRecruiterProfile().getUser();
        String notifTitle = "Actualización de Convenio de Prácticas";
        String notifContent = "La institución ha " +
                (request.getStatus() == AgreementStatus.APPROVED ? "APROBADO" : "RECHAZADO") +
                " el convenio: " + agreement.getTitle() + ".";

        notificationService.createNotification(postulantUser, notifTitle, notifContent);
        notificationService.createNotification(recruiterUser, notifTitle, notifContent);

        return AgreementResponseDTO.builder()
                .id(agreement.getId())
                .status(agreement.getStatus())
                .observations(agreement.getObservations())
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

        // Ejecutar las consultas agregadas
        long verifiedStudents = postulantRepository.countByInstitutionProfileIdAndIsInstitutionVerifiedTrue(instId);
        long hiredStudents = postulantRepository.countHiredStudentsByInstitutionId(instId);
        long activeAgreements = agreementRepository.countByInstitutionProfileIdAndStatus(instId, AgreementStatus.APPROVED);
        long endorsedCompanies = endorsementRepository.countByInstitutionProfileId(instId);

        // Calcular el porcentaje de inserción laboral (evitando división por cero)
        double rate = 0.0;
        if (verifiedStudents > 0) {
            rate = ((double) hiredStudents / verifiedStudents) * 100;
        }

        return DashboardStatsResponseDTO.builder()
                .totalVerifiedStudents(verifiedStudents)
                .totalHiredStudents(hiredStudents)
                .totalActiveAgreements(activeAgreements)
                .totalEndorsedCompanies(endorsedCompanies)
                // Redondear a dos decimales
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
    public String updateSkillValidationStatus(String email, UUID validationId, ValidationUpdateRequestDTO request) {
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

        // Envio de notificación al postulante
        String notifTitle = "Actualización de Habilidad";
        String notifContent = "Tu solicitud de validación académica para la habilidad '" +
                validation.getTechnicalSkill().getName() + "' ha sido " +
                (request.getStatus().equals("APPROVED") ? "APROBADA" : "RECHAZADA") + "." +
                " " + validation.getObservation();
        notificationService.createNotification(validation.getPostulantProfile().getUser(), notifTitle, notifContent);

        return "El estado de la validación se ha actualizado correctamente.";
    }
}
