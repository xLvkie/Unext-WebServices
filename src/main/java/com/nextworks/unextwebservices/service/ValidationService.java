package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.ValidationRequestDTO;
import com.nextworks.unextwebservices.dto.ValidationResponseDTO;
import com.nextworks.unextwebservices.entity.AcademicValidation;
import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.AcademicValidationRepository;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
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
                // No definimos su estado debido a @PrePersist de la entidad (ya nace con un valor)
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
}
