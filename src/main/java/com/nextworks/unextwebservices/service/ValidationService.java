package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.validation.AcademicVerificationRequestDTO;
import com.nextworks.unextwebservices.dto.validation.ValidationRequestDTO;
import com.nextworks.unextwebservices.dto.validation.ValidationResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.entity.enums.ValidationStatus;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final AcademicValidationRepository validationRepository;
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final NotificationService notificationService;

    /* ==============================================================
    // 1. SOLICITAR VALIDACIÓN DE UNA HABILIDAD TÉCNICA
    // ============================================================== */
    @Transactional
    public ValidationResponseDTO requestValidation(String email, ValidationRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        if (!Boolean.TRUE.equals(postulant.getIsInstitutionVerified())) {
            throw new RuntimeException("HTTP 403: Primero debes verificar tu vínculo académico con una institución para poder certificar tus habilidades.");
        }

        InstitutionProfile institution = institutionRepository.findById(request.getInstitutionProfileId())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        StudentSkill skill = studentSkillRepository.findById(request.getTechnicalSkillId())
                .orElseThrow(() -> new RuntimeException("Habilidad no encontrada"));

        if (!skill.getPostulantProfile().getId().equals(postulant.getId())) {
            throw new RuntimeException("No puedes validar una habilidad que no está en tu perfil.");
        }

        AcademicValidation validation = AcademicValidation.builder()
                .postulantProfile(postulant)
                .institutionProfile(institution)
                .technicalSkill(skill)
                .evidenceUrl(request.getEvidenceUrl())
                .status(ValidationStatus.PENDING)
                .build();

        validationRepository.save(validation);

        // Gatillo de notificación hacia la institución (PEDIENTE EN EL FRONTEND)
        User institutionUser = institution.getUser();
        String notifTitle = "Nueva validación de habilidad recibida";
        String notifContent = "El postulante " + postulant.getFirstName() + " " + postulant.getLastName() +
                " ha solicitado certificar su habilidad: '" + skill.getName() + "'.";
        notificationService.createNotification(institutionUser, notifTitle, notifContent);

        return mapToDTO(validation);
    }

    @Transactional(readOnly = true)
    public List<ValidationResponseDTO> getMyValidations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        return validationRepository.findByPostulantProfileId(postulant.getId()).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /* ==============================================================
    // 2. SOLICITAR VERIFICACIÓN ACADÉMICA
    // ============================================================== */
    @Transactional
    public String requestAcademicVerification(String email, AcademicVerificationRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        InstitutionProfile institution = institutionRepository.findById(request.getInstitutionProfileId())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        postulant.setInstitutionProfile(institution);
        postulant.setIsInstitutionVerified(false);
        postulant.setAcademicStatus(request.getAcademicStatus());

        if ("STUDENT".equalsIgnoreCase(request.getAcademicStatus())) {
            // Caso 2: Estudiante Universitario Activo
            if (request.getStudentCode() == null || request.getStudentCode().isBlank() ||
                    request.getCareer() == null || request.getCareer().isBlank() ||
                    request.getInstitutionalEmail() == null || request.getInstitutionalEmail().isBlank()) {
                throw new RuntimeException("HTTP 400: Para estudiantes activos, el correo institucional, código y carrera son obligatorios.");
            }
            postulant.setHasUniversityBase(true);
            postulant.setInstitutionalEmail(request.getInstitutionalEmail().trim());
            postulant.setStudentCode(request.getStudentCode().trim());
            postulant.setCareer(request.getCareer().trim());
            postulant.setCurrentCycle(request.getCurrentCycle() != null ? request.getCurrentCycle() : 1);
        } else {
            // Caso 1: Egresado / Ya terminó su carrera (Valida mediante documento/diploma)
            if (request.getAcademicEvidenceUrl() == null || request.getAcademicEvidenceUrl().isBlank()) {
                throw new RuntimeException("HTTP 400: Debes adjuntar tu constancia de egreso, bachiller o carné como documento de sustento.");
            }
            postulant.setHasUniversityBase(false);
            postulant.setAcademicEvidenceUrl(request.getAcademicEvidenceUrl());
            postulant.setAcademicEvidenceName(request.getAcademicEvidenceName());
            if (request.getCareer() != null && !request.getCareer().isBlank()) {
                postulant.setCareer(request.getCareer().trim());
            }
            postulant.setCurrentCycle(null);
        }

        postulantRepository.save(postulant);

        // Gatillo de notificación para la institución (FALTA AGREGAR EN EL FRONTEND)
        notificationService.createNotification(
                institution.getUser(),
                "Nueva solicitud de verificación académica",
                "El postulante " + postulant.getFirstName() + " " + postulant.getLastName() +
                        " solicitó verificación académica (" + request.getAcademicStatus() + ")."
        );

        return "Solicitud de verificación académica enviada a " + institution.getName() + ".";
    }

    @Transactional
    public String linkInstitution(String email, UUID institutionId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        InstitutionProfile institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        postulant.setInstitutionProfile(institution);
        postulant.setIsInstitutionVerified(false);
        postulantRepository.save(postulant);

        return "Solicitud de vinculación enviada exitosamente a la institución.";
    }

    private ValidationResponseDTO mapToDTO(AcademicValidation val) {
        return ValidationResponseDTO.builder()
                .id(val.getId())
                .technicalSkillId(val.getTechnicalSkill().getId())
                .institutionName(val.getInstitutionProfile().getName())
                .institutionLogoUrl(val.getInstitutionProfile().getLogoUrl())
                .technicalSkillName(val.getTechnicalSkill().getName())
                .evidenceUrl(val.getEvidenceUrl())
                .status(val.getStatus())
                .build();
    }
}
