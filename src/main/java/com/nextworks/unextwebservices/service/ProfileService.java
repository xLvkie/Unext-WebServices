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
public class ProfileService {
    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final StudentSkillRepository studentSkillRepository;

    /* ====================================
    // Creación del perfil de los segmentos
    // ==================================== */
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
                .industry(request.getIndustry())
                .description(request.getDescription())
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

        InstitutionProfile profile = InstitutionProfile.builder()
                .user(user)
                .name(request.getInstitutionName())
                .domain(request.getDomain())
                .build();

        institutionRepository.save(profile);

        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de institución creado exitosamente";
    }

    /* ============================================
    // Mostrar y editar datos del perfil postulante
    // ============================================ */
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
        return getMyPostulantProfile(email);
    }

    /* ============================================
    // Mostrar y editar datos del perfil postulante
    // ============================================ */
    public RecruiterProfileResponseDTO getMyRecruiterProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de reclutador no encontrado"));

        return RecruiterProfileResponseDTO.builder()
                .id(profile.getId())
                .companyName(profile.getCompanyName())
                .ruc(profile.getRuc())
                .industry(profile.getIndustry())
                .description(profile.getDescription())
                .isValidated(profile.getIsValidated())
                .build();
    }

    @Transactional
    public RecruiterProfileResponseDTO updateMyRecruiterProfile(String email, RecruiterProfileUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de reclutador no encontrado"));

        if (request.getCompanyName() != null) profile.setCompanyName(request.getCompanyName());
        if (request.getIndustry() != null) profile.setIndustry(request.getIndustry());
        if (request.getDescription() != null) profile.setDescription(request.getDescription());

        recruiterRepository.save(profile);
        return getMyRecruiterProfile(email);
    }

    /* ============================================
    // Mostrar y editar datos del perfil postulante
    // ============================================ */
    public InstitutionProfileResponseDTO getMyInstitutionProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        InstitutionProfile profile = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        return InstitutionProfileResponseDTO.builder()
                .id(profile.getId())
                .name(profile.getName())
                .domain(profile.getDomain())
                .logoUrl(profile.getLogoUrl())
                .description(profile.getDescription())
                .build();
    }

    @Transactional
    public InstitutionProfileResponseDTO updateMyInstitutionProfile(String email, InstitutionProfileUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        InstitutionProfile profile = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        if (request.getName() != null) profile.setName(request.getName());
        if (request.getDomain() != null) profile.setDomain(request.getDomain());
        if (request.getLogoUrl() != null) profile.setLogoUrl(request.getLogoUrl());
        if (request.getDescription() != null) profile.setDescription(request.getDescription());

        institutionRepository.save(profile);
        return getMyInstitutionProfile(email);
    }

    /* ===================================
    // Tabla de Habilidades del Postulante
    // =================================== */
    @Transactional
    public StudentSkillResponseDTO addSkill(String email, StudentSkillRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        StudentSkill skill = StudentSkill.builder()
                .postulantProfile(profile)
                .name(request.getName())
                .masteryLevel(request.getMasteryLevel())
                .isValidatedByInstitution(false)
                .build();

        studentSkillRepository.save(skill);

        return StudentSkillResponseDTO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .masteryLevel(skill.getMasteryLevel())
                .isValidatedByInstitution(skill.getIsValidatedByInstitution())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StudentSkillResponseDTO> getMySkills(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        List<StudentSkill> skills = studentSkillRepository.findByPostulantProfileId(profile.getId());

        return skills.stream()
                .map(skill -> StudentSkillResponseDTO.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .masteryLevel(skill.getMasteryLevel())
                        .isValidatedByInstitution(skill.getIsValidatedByInstitution())
                        .build())
                .toList();
    }

    @Transactional
    public StudentSkillResponseDTO updateSkill(String email, UUID skillId, StudentSkillUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        StudentSkill skill = studentSkillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Habilidad no encontrada"));

        if (!skill.getPostulantProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("No tienes permiso para editar esta habilidad");
        }

        if (request.getName() != null) skill.setName(request.getName());
        if (request.getMasteryLevel() != null) skill.setMasteryLevel(request.getMasteryLevel());

        studentSkillRepository.save(skill);

        return StudentSkillResponseDTO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .masteryLevel(skill.getMasteryLevel())
                .isValidatedByInstitution(skill.getIsValidatedByInstitution())
                .build();
    }

    @Transactional
    public void deleteSkill(String email, UUID skillId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        StudentSkill skill = studentSkillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Habilidad no encontrada"));

        // Validamos por seguridad que la habilidad pertenezca al estudiante que intenta borrarla
        if (!skill.getPostulantProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar esta habilidad");
        }

        studentSkillRepository.delete(skill);
    }
}
