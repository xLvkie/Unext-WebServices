package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.directory.PostulantDirectoryResponseDTO;
import com.nextworks.unextwebservices.dto.profile.*;
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
    public String createPostulantProfile(PostulantProfileRequestDTO request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getIsProfileCompleted()) {
            throw new RuntimeException("El perfil ya está completado");
        }

        PostulantProfile profile = PostulantProfile.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .studentCode(request.getStudentCode())
                .career(request.getCareer())
                .currentCycle(request.getCurrentCycle())
                .hasUniversityBase(true)
                .build();

        postulantRepository.save(profile);

        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de postulante creado exitosamente";
    }

    @Transactional
    public String createRecruiterProfile(RecruiterProfileRequestDTO request, String email) {
        User user = userRepository.findByEmail(email)
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
    public String createInstitutionProfile(InstitutionProfileRequestDTO request, String email) {
        User user = userRepository.findByEmail(email)
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

        List<StudentSkill> skills = studentSkillRepository.findByPostulantProfileId(profile.getId());
        List<StudentSkillResponseDTO> skillsDTO = skills.stream()
                .map(skill -> StudentSkillResponseDTO.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .masteryLevel(skill.getMasteryLevel())
                        .build())
                .toList();

        return PostulantProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .studentCode(profile.getStudentCode())
                .career(profile.getCareer())
                .currentCycle(profile.getCurrentCycle())
                .cvUrl(profile.getCvUrl())
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .hasUniversityBase(profile.getHasUniversityBase())
                .isInstitutionVerified(profile.getIsInstitutionVerified())
                .institutionProfile(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getId() : null)
                .skills(skillsDTO)
                .build();
    }

    @Transactional
    public PostulantProfileResponseDTO updateMyPostulantProfile(String email, PostulantProfileUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        if (request.getCareer() != null) profile.setCareer(request.getCareer());
        if (request.getCurrentCycle() != null) profile.setCurrentCycle(request.getCurrentCycle());
        if (request.getCvUrl() != null) profile.setCvUrl(request.getCvUrl());
        if (request.getHeadline() != null) profile.setHeadline(request.getHeadline());
        if (request.getBio() != null) profile.setBio(request.getBio());

        postulantRepository.save(profile);
        return getMyPostulantProfile(email);
    }

    /* ============================================
    // Mostrar y editar datos del perfil reclutador
    // ============================================ */
    public RecruiterProfileResponseDTO getMyRecruiterProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de reclutador no encontrado"));

        return RecruiterProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
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
    // Mostrar y editar datos del perfil intitucion
    // ============================================ */
    public InstitutionProfileResponseDTO getMyInstitutionProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        InstitutionProfile profile = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de institución no encontrado"));

        return InstitutionProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
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

        if (studentSkillRepository.existsByPostulantProfileIdAndNameIgnoreCase(profile.getId(), request.getName())) {
            throw new RuntimeException("Ya tienes una habilidad técnica registrada con el nombre: " + request.getName());
        }

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

    // LISTADO DE HABILIDADES
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

    // ACTUALIZAR HABILIDAD TECNICA
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

    // ELIMINAR HABILIDAD TECNICA
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

    // LISTADO DE TODOS LOS POSTULANTES
    @Transactional(readOnly = true)
    public List<PostulantDirectoryResponseDTO> getAllPostulants() {
        List<PostulantProfile> postulants = postulantRepository.findAll();

        return postulants.stream().map(profile -> PostulantDirectoryResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .career(profile.getCareer())
                .isInstitutionVerified(profile.getIsInstitutionVerified())
                .build()
        ).toList();
    }

    // PERFIL ESPECIFICO DE UN POSTULANTE
    @Transactional(readOnly = true)
    public PostulantProfileResponseDTO getPostulantProfileById(UUID postulantId) {
        PostulantProfile profile = postulantRepository.findById(postulantId)
                .orElseThrow(() -> new RuntimeException("HTTP 404: Perfil de postulante no encontrado"));
        
        List<StudentSkill> skills = studentSkillRepository.findByPostulantProfileId(profile.getId());
        List<StudentSkillResponseDTO> skillsDTO = skills.stream()
                .map(skill -> StudentSkillResponseDTO.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .masteryLevel(skill.getMasteryLevel())
                        .build())
                .toList();

        return PostulantProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .studentCode(profile.getStudentCode())
                .career(profile.getCareer())
                .currentCycle(profile.getCurrentCycle())
                .cvUrl(profile.getCvUrl())
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .hasUniversityBase(profile.getHasUniversityBase())
                .isInstitutionVerified(profile.getIsInstitutionVerified())
                .institutionProfile(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getId() : null)
                .skills(skillsDTO)
                .build();
    }
}
