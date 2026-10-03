package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.ValidationRequestDTO;
import com.nextworks.unextwebservices.dto.ValidationResponseDTO;
import com.nextworks.unextwebservices.entity.AcademicValidation;
import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.AcademicValidationRepository;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import com.nextworks.unextwebservices.dto.ValidationStatusUpdateDTO;
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final AcademicValidationRepository validationRepository;
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final JobApplicationRepository jobApplicationRepository;

    @Transactional
    public ValidationResponseDTO requestValidation(String email, ValidationRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile postulant = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        InstitutionProfile institution = institutionRepository.findById(request.getInstitutionProfileId())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        AcademicValidation validation = AcademicValidation.builder()
                .postulantProfile(postulant)
                .institutionProfile(institution)
                .knowledgeTitle(request.getKnowledgeTitle())
                .evidenceUrl(request.getEvidenceUrl())
                // El estado nace en PENDING automáticamente gracias al @PrePersist de tu entidad
                .build();

        validationRepository.save(validation);

        return ValidationResponseDTO.builder()
                .id(validation.getId())
                .institutionName(institution.getName())
                .knowledgeTitle(validation.getKnowledgeTitle())
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
                        .knowledgeTitle(val.getKnowledgeTitle())
                        .evidenceUrl(val.getEvidenceUrl())
                        .status(val.getStatus())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ValidationResponseDTO> getInstitutionValidations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        List<AcademicValidation> validations = validationRepository.findByInstitutionProfileId(institution.getId());

        return validations.stream()
                .map(val -> ValidationResponseDTO.builder()
                        .id(val.getId())
                        .institutionName(val.getInstitutionProfile().getName())
                        .knowledgeTitle(val.getKnowledgeTitle())
                        .evidenceUrl(val.getEvidenceUrl())
                        .status(val.getStatus())
                        .build())
                .toList();
    }

    @Transactional
    public ValidationResponseDTO updateValidationStatus(String email, UUID validationId, ValidationStatusUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        AcademicValidation validation = validationRepository.findById(validationId)
                .orElseThrow(() -> new RuntimeException("Validación no encontrada"));

        // Verificar que la validación pertenezca a esta institución
        if (!validation.getInstitutionProfile().getId().equals(institution.getId())) {
            throw new RuntimeException("HTTP 403: No tienes permiso para actualizar esta validación");
        }

        validation.setStatus(request.getStatus());
        validationRepository.save(validation);

        return ValidationResponseDTO.builder()
                .id(validation.getId())
                .institutionName(validation.getInstitutionProfile().getName())
                .knowledgeTitle(validation.getKnowledgeTitle())
                .evidenceUrl(validation.getEvidenceUrl())
                .status(validation.getStatus())
                .build();
    }

}
