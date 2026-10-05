package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.ValidationRequestDTO;
import com.nextworks.unextwebservices.dto.ValidationResponseDTO;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final AcademicValidationRepository validationRepository;
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final NotificationService notificationService;

    @Transactional
    public ValidationResponseDTO requestValidation(String email, ValidationRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

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
                .build();

        validationRepository.save(validation);

        // Envio de notificacion
        User institutionUser = institution.getUser();
        String notifTitle = "Nueva validación de habilidades recibida";
        String notifContent = "El postulante " + postulant.getFirstName() + " " + postulant.getLastName() +
                " ha solicitado la validación de su habilidad: '" + skill.getName() + "'.";
        notificationService.createNotification(institutionUser, notifTitle, notifContent);

        return ValidationResponseDTO.builder()
                .id(validation.getId())
                .institutionName(institution.getName())
                .technicalSkillName(validation.getTechnicalSkill().getName())
                .evidenceUrl(validation.getEvidenceUrl())
                .status(validation.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ValidationResponseDTO> getMyValidations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        List<AcademicValidation> validations = validationRepository.findByPostulantProfileId(postulant.getId());

        return validations.stream()
                .map(val -> ValidationResponseDTO.builder()
                        .id(val.getId())
                        .institutionName(val.getInstitutionProfile().getName())
                        .technicalSkillName(val.getTechnicalSkill().getName())
                        .evidenceUrl(val.getEvidenceUrl())
                        .status(val.getStatus())
                        .build())
                .toList();
    }
}
