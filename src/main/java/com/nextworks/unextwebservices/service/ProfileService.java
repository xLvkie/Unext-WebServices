package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.InstitutionProfileRequestDTO;
import com.nextworks.unextwebservices.dto.PostulantProfileRequestDTO;
import com.nextworks.unextwebservices.dto.RecruiterProfileRequestDTO;
import com.nextworks.unextwebservices.dto.PostulantProfileResponseDTO;
import com.nextworks.unextwebservices.dto.PostulantProfileUpdateDTO;

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

    /* ==================================
    // Creación del perfil del postulante
    // ================================== */
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

    /* ==================================
    // Creación del perfil del reclutador
    // ================================== */
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

    /* ===================================
    // Creación del perfil del institucion
    // =================================== */
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

    /* ===================================
    // Mostrar datos del perfil postulante
    // =================================== */
    @Transactional(readOnly = true)
    public PostulantProfileResponseDTO getMyPostulantProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        return PostulantProfileResponseDTO.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .studentCode(profile.getStudentCode())
                .career(profile.getCareer())
                .currentCycle(profile.getCurrentCycle())
                .cvUrl(profile.getCvUrl())
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .hasUniversityBase(profile.getHasUniversityBase())
                .build();
    }

    /* ===================================
    // Editar datos del perfil postulante
    // =================================== */
    @Transactional
    public PostulantProfileResponseDTO updateMyPostulantProfile(String email, PostulantProfileUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        // Actualizamos solo los campos permitidos si vienen en la petición
        if (request.getCareer() != null) profile.setCareer(request.getCareer());
        if (request.getCurrentCycle() != null) profile.setCurrentCycle(request.getCurrentCycle());
        if (request.getCvUrl() != null) profile.setCvUrl(request.getCvUrl());
        if (request.getHeadline() != null) profile.setHeadline(request.getHeadline());
        if (request.getBio() != null) profile.setBio(request.getBio());

        postulantRepository.save(profile);

        // Reutilizamos el método anterior para devolver el perfil actualizado
        return getMyPostulantProfile(email);
    }
}
