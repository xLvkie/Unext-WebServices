package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.InstitutionProfileRequestDTO;
import com.nextworks.unextwebservices.dto.PostulantProfileRequestDTO;
import com.nextworks.unextwebservices.dto.RecruiterProfileRequestDTO;

import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.RecruiterProfile;
import com.nextworks.unextwebservices.entity.User;

import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.RecruiterProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InstitutionProfileRepository institutionRepository;

    @Transactional
    public String createPostulantProfile(UUID userId, PostulantProfileRequestDTO request) {
        // 1. Buscar al usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Validar que no tenga el perfil ya creado
        if (user.getIsProfileCompleted()) {
            throw new RuntimeException("El perfil ya está completado");
        }

        // 3. Crear la entidad del perfil
        PostulantProfile profile = PostulantProfile.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .studentCode(request.getStudentCode())
                .career(request.getCareer())
                .currentCycle(request.getCurrentCycle())
                .hasUniversityBase(true) // Por defecto para esta versión
                .build();

        // 4. Guardar el perfil en la base de datos
        postulantRepository.save(profile);

        // 5. Actualizar el estado del usuario
        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de postulante creado exitosamente";
    }

    @Transactional
    public String createRecruiterProfile(UUID userId, RecruiterProfileRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getIsProfileCompleted()) {
            throw new RuntimeException("El perfil ya está completado");
        }

        RecruiterProfile profile = RecruiterProfile.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .ruc(request.getRuc())
                .industry(request.getBusinessSector())
                .description("Cargo corporativo del usuario: " + request.getCorporatePosition())
                .isValidated(false)
                .build();

        recruiterRepository.save(profile);

        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de reclutador creado exitosamente";
    }

    @Transactional
    public String createInstitutionProfile(UUID userId, InstitutionProfileRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getIsProfileCompleted()) {
            throw new RuntimeException("El perfil ya está completado");
        }

        String domainExtracted = request.getContactEmail().substring(request.getContactEmail().indexOf("@") + 1);

        InstitutionProfile profile = InstitutionProfile.builder()
                .user(user)
                .name(request.getInstitutionName())
                .domain(domainExtracted)
                .description("Representante: " + request.getRepresentativeName() +
                        " | Código Institucional: " + request.getInstitutionalCode())
                .build();

        institutionRepository.save(profile);

        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de institución creado exitosamente";
    }
}
