package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.directory.InstitutionDirectoryResponseDTO;
import com.nextworks.unextwebservices.dto.directory.PostulantDirectoryResponseDTO;
import com.nextworks.unextwebservices.dto.directory.RecruiterDirectoryResponseDTO;
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

        if (Boolean.TRUE.equals(user.getIsProfileCompleted())) {
            throw new RuntimeException("El perfil ya está completado");
        }

        PostulantProfile profile = PostulantProfile.builder()
                .user(user)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .headline(request.getHeadline())
                .bio(request.getBio())
                .cvUrl(request.getCvUrl())
                .avatarUrl(request.getAvatarUrl())
                .bannerUrl(request.getBannerUrl())
                .bannerText(request.getBannerText())
                .cvUrl(request.getCvUrl())
                .cvFileName(request.getCvFileName())
                .cvFileSize(request.getCvFileSize())
                // Campos universitarios inician nulos/falsos hasta que solicite "Verificación Universitaria"
                .studentCode(request.getStudentCode())
                .career(request.getCareer())
                .currentCycle(request.getCurrentCycle())
                .hasUniversityBase(false)
                .isInstitutionVerified(false)
                .isVerified(true)
                .build();

        postulantRepository.save(profile);

        user.setIsProfileCompleted(true);
        user.setIsEmailVerified(false);
        userRepository.save(user);

        return "Perfil de postulante creado y activado exitosamente";
    }

    @Transactional
    public String createRecruiterProfile(RecruiterProfileRequestDTO request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getIsProfileCompleted()) {
            throw new RuntimeException("El perfil ya está completado");
        }

        // Validación de formato RUC (11 dígitos, empieza con 10 o 20)
        if (request.getRuc() == null || !request.getRuc().matches("^(10|20)\\d{9}$")) {
            throw new RuntimeException("HTTP 400: El RUC debe tener 11 dígitos válidos y comenzar con 10 o 20.");
        }

        if (recruiterRepository.existsByRuc(request.getRuc())) {
            throw new RuntimeException("HTTP 409: Ya existe una empresa registrada con este RUC.");
        }

        // Generamos código OTP de 6 dígitos para verificación corporativa
        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));

        RecruiterProfile profile = RecruiterProfile.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .ruc(request.getRuc())
                .industry(request.getIndustry())
                .description(request.getDescription())
                .isValidated(false)
                .isVerified(false) // Permanece oculto hasta verificar el código
                .verificationCode(otpCode)
                .build();

        recruiterRepository.save(profile);

        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de reclutador creado. Cuenta pendiente de verificación corporativa. Código de prueba: " + otpCode;
    }

    @Transactional
    public String createInstitutionProfile(InstitutionProfileRequestDTO request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getIsProfileCompleted()) {
            throw new RuntimeException("El perfil ya está completado");
        }

        String cleanDomain = request.getDomain().trim().toLowerCase();
        if (institutionRepository.existsByDomain(cleanDomain)) {
            throw new RuntimeException("HTTP 409: Este dominio institucional ya está registrado.");
        }

        // Generamos código OTP de 6 dígitos para verificación institucional
        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));

        InstitutionProfile profile = InstitutionProfile.builder()
                .user(user)
                .name(request.getInstitutionName())
                .domain(cleanDomain)
                .isVerified(false) // Permanece oculto hasta verificar el código
                .verificationCode(otpCode)
                .build();

        institutionRepository.save(profile);

        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de institución creado. Cuenta pendiente de verificación. Código de prueba: " + otpCode;
    }

    /* =======================================================
    // Verificación de Organizaciones (Reclutador / Institución)
    // ======================================================= */
    @Transactional
    public String verifyOrganization(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String role = user.getRole().name();

        if (role.equals("RECRUITER")) {
            RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Primero debes completar los datos de tu perfil de empresa"));

            if (Boolean.TRUE.equals(profile.getIsVerified())) {
                return "Tu cuenta corporativa ya se encuentra verificada";
            }

            if (profile.getVerificationCode() == null || !profile.getVerificationCode().equals(code.trim())) {
                throw new RuntimeException("HTTP 400: Código de verificación corporativo incorrecto");
            }

            profile.setIsVerified(true);
            profile.setVerificationCode(null);
            recruiterRepository.save(profile);

        } else if (role.equals("INSTITUTION")) {
            InstitutionProfile profile = institutionRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Primero debes completar los datos de tu perfil institucional"));

            if (Boolean.TRUE.equals(profile.getIsVerified())) {
                return "Tu cuenta institucional ya se encuentra verificada";
            }

            if (profile.getVerificationCode() == null || !profile.getVerificationCode().equals(code.trim())) {
                throw new RuntimeException("HTTP 400: Código de verificación institucional incorrecto");
            }

            profile.setIsVerified(true);
            profile.setVerificationCode(null);
            institutionRepository.save(profile);
        } else {
            throw new RuntimeException("HTTP 403: Rol no aplicable para verificación organizacional");
        }

        user.setIsEmailVerified(true);
        userRepository.save(user);

        return "Organización verificada exitosamente. Tu perfil ahora es visible públicamente.";
    }

    /* ============================================
    // Mostrar y editar datos del perfil postulante
    // ============================================ */
    @Transactional(readOnly = true)
    public PostulantProfileResponseDTO getMyPostulantProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("HTTP 404: Perfil de postulante no encontrado"));

        List<StudentSkill> mySkills = studentSkillRepository.findByPostulantProfileId(profile.getId());
        List<StudentSkillResponseDTO> mySkillsDTO = mySkills.stream()
                .map(skill -> StudentSkillResponseDTO.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .masteryLevel(skill.getMasteryLevel())
                        .isTopSkill(Boolean.TRUE.equals(skill.getIsTopSkill()))
                        .isValidatedByInstitution(Boolean.TRUE.equals(skill.getIsValidatedByInstitution()))
                        .validatedInstitutionName(skill.getValidatedInstitutionName())
                        .validatedInstitutionLogoUrl(skill.getValidatedInstitutionLogoUrl())
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
                .institutionalEmail(profile.getInstitutionalEmail())
                .academicStatus(profile.getAcademicStatus())
                .academicEvidenceUrl(profile.getAcademicEvidenceUrl())
                .academicEvidenceName(profile.getAcademicEvidenceName())
                .avatarUrl(profile.getAvatarUrl())
                .bannerUrl(profile.getBannerUrl())
                .bannerText(profile.getBannerText())
                .cvUrl(profile.getCvUrl())
                .cvFileName(profile.getCvFileName())
                .cvFileSize(profile.getCvFileSize())
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .hasUniversityBase(Boolean.TRUE.equals(profile.getHasUniversityBase()))
                .isInstitutionVerified(Boolean.TRUE.equals(profile.getIsInstitutionVerified()))
                .institutionProfile(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getId() : null)
                .institutionName(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getName() : null)
                .institutionLogoUrl(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getLogoUrl() : null)
                .skills(mySkillsDTO)
                .build();
    }

    @Transactional
    public PostulantProfileResponseDTO updateMyPostulantProfile(String email, PostulantProfileUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de postulante no encontrado"));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getCareer() != null) profile.setCareer(request.getCareer());
        if (request.getCurrentCycle() != null) profile.setCurrentCycle(request.getCurrentCycle());
        if (request.getCvUrl() != null) profile.setCvUrl(request.getCvUrl());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getBannerUrl() != null) profile.setBannerUrl(request.getBannerUrl());
        if (request.getBannerText() != null) profile.setBannerText(request.getBannerText());
        if (request.getCvUrl() != null) profile.setCvUrl(request.getCvUrl());
        if (request.getCvFileName() != null) profile.setCvFileName(request.getCvFileName());
        if (request.getCvFileSize() != null) profile.setCvFileSize(request.getCvFileSize());
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

        boolean wantsTopSkill = Boolean.TRUE.equals(request.getIsTopSkill());
        if (wantsTopSkill) {
            long currentTopCount = studentSkillRepository.countByPostulantProfileIdAndIsTopSkillTrue(profile.getId());
            if (currentTopCount >= 3) {
                throw new RuntimeException("HTTP 400: Solo puedes destacar un máximo de 3 aptitudes principales.");
            }
        }

        StudentSkill skill = StudentSkill.builder()
                .postulantProfile(profile)
                .name(request.getName().trim())
                .masteryLevel(request.getMasteryLevel())
                .isTopSkill(wantsTopSkill)
                .isValidatedByInstitution(false)
                .build();

        studentSkillRepository.save(skill);

        return StudentSkillResponseDTO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .masteryLevel(skill.getMasteryLevel())
                .isTopSkill(Boolean.TRUE.equals(skill.getIsTopSkill()))
                .isValidatedByInstitution(Boolean.TRUE.equals(skill.getIsValidatedByInstitution()))
                .validatedInstitutionName(skill.getValidatedInstitutionName())
                .validatedInstitutionLogoUrl(skill.getValidatedInstitutionLogoUrl())
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
                        .isTopSkill(Boolean.TRUE.equals(skill.getIsTopSkill()))
                        .isValidatedByInstitution(Boolean.TRUE.equals(skill.getIsValidatedByInstitution()))
                        .validatedInstitutionName(skill.getValidatedInstitutionName())
                        .validatedInstitutionLogoUrl(skill.getValidatedInstitutionLogoUrl())
                        .build())
                .toList();
    }

    // ACTUALIZAR HABILIDAD TECNICA (Incluye marcar/desmarcar como Principal Aptitud con tope de 3)
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

        if (request.getIsTopSkill() != null) {
            boolean markingAsTop = Boolean.TRUE.equals(request.getIsTopSkill());
            boolean wasAlreadyTop = Boolean.TRUE.equals(skill.getIsTopSkill());

            if (markingAsTop && !wasAlreadyTop) {
                long currentTopCount = studentSkillRepository.countByPostulantProfileIdAndIsTopSkillTrue(profile.getId());
                if (currentTopCount >= 3) {
                    throw new RuntimeException("HTTP 400: Ya tienes 3 aptitudes principales seleccionadas. Desmarca una primero.");
                }
            }
            skill.setIsTopSkill(markingAsTop);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            skill.setName(request.getName().trim());
        }
        if (request.getMasteryLevel() != null) {
            skill.setMasteryLevel(request.getMasteryLevel());
        }

        studentSkillRepository.save(skill);

        return StudentSkillResponseDTO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .masteryLevel(skill.getMasteryLevel())
                .isTopSkill(Boolean.TRUE.equals(skill.getIsTopSkill()))
                .isValidatedByInstitution(Boolean.TRUE.equals(skill.getIsValidatedByInstitution()))
                .validatedInstitutionName(skill.getValidatedInstitutionName())
                .validatedInstitutionLogoUrl(skill.getValidatedInstitutionLogoUrl())
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

    /* ================================
        LISTADO O BUSQUEDA DE SEGMENTOS
    // ================================= */

    // PERFIL ESPECIFICO DE UN POSTULANTE (Solo si está verificado)
    @Transactional(readOnly = true)
    public PostulantProfileResponseDTO getPostulantProfileById(UUID postulantId) {
        PostulantProfile profile = postulantRepository.findByIdAndIsVerifiedTrue(postulantId)
                .orElseThrow(() -> new RuntimeException("HTTP 404: Perfil de postulante no encontrado o no verificado"));

        List<StudentSkill> skills = studentSkillRepository.findByPostulantProfileId(profile.getId());
        List<StudentSkillResponseDTO> skillsDTO = skills.stream()
                .map(skill -> StudentSkillResponseDTO.builder()
                        .id(skill.getId())
                        .name(skill.getName())
                        .masteryLevel(skill.getMasteryLevel())
                        .isTopSkill(Boolean.TRUE.equals(skill.getIsTopSkill()))
                        .isValidatedByInstitution(Boolean.TRUE.equals(skill.getIsValidatedByInstitution()))
                        .validatedInstitutionName(skill.getValidatedInstitutionName())
                        .validatedInstitutionLogoUrl(skill.getValidatedInstitutionLogoUrl())
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
                .institutionalEmail(profile.getInstitutionalEmail())
                .academicStatus(profile.getAcademicStatus())
                .academicEvidenceUrl(profile.getAcademicEvidenceUrl())
                .academicEvidenceName(profile.getAcademicEvidenceName())
                .avatarUrl(profile.getAvatarUrl())
                .bannerUrl(profile.getBannerUrl())
                .bannerText(profile.getBannerText())
                .cvUrl(profile.getCvUrl())
                .cvFileName(profile.getCvFileName())
                .cvFileSize(profile.getCvFileSize())
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .hasUniversityBase(Boolean.TRUE.equals(profile.getHasUniversityBase()))
                .isInstitutionVerified(Boolean.TRUE.equals(profile.getIsInstitutionVerified()))
                .institutionProfile(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getId() : null)
                .institutionName(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getName() : null)
                .institutionLogoUrl(profile.getInstitutionProfile() != null ? profile.getInstitutionProfile().getLogoUrl() : null)
                .skills(skillsDTO)
                .build();
    }

    // LISTADO DE TODOS LOS POSTULANTES (Solo perfiles verificados/completados)
    @Transactional(readOnly = true)
    public List<PostulantDirectoryResponseDTO> getAllPostulants() {
        List<PostulantProfile> postulants = postulantRepository.findByIsVerifiedTrue();

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

    // LISTADO DE TODOS LAS INSTITUCIONES
    @Transactional(readOnly = true)
    public List<InstitutionDirectoryResponseDTO> getAllInstitutions() {
        List<InstitutionProfile> institutions = institutionRepository.findByIsVerifiedTrue();

        return institutions.stream().map(profile -> InstitutionDirectoryResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getName())
                .domain(profile.getDomain())
                .logoUrl(profile.getLogoUrl())
                .description(profile.getDescription())
                .build()
        ).toList();
    }

    // PERFIL ESPECIFICO DE UNA INSTITUCION (Solo si está verificada institucionalmente)
    @Transactional(readOnly = true)
    public InstitutionProfileResponseDTO getInstitutionProfileById(UUID institutionId) {
        InstitutionProfile profile = institutionRepository.findByIdAndIsVerifiedTrue(institutionId)
                .orElseThrow(() -> new RuntimeException("HTTP 404: Perfil de institución no encontrado o pendiente de verificación"));

        return InstitutionProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getName())
                .domain(profile.getDomain())
                .logoUrl(profile.getLogoUrl())
                .description(profile.getDescription())
                .build();
    }

    // LISTADO DE TODOS LOS RECLUTADORES
    @Transactional(readOnly = true)
    public List<RecruiterDirectoryResponseDTO> getAllRecruiters() {
        List<RecruiterProfile> recruiters = recruiterRepository.findByIsVerifiedTrue();

        return recruiters.stream().map(profile -> RecruiterDirectoryResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .companyName(profile.getCompanyName())
                .build()
        ).toList();
    }

    // PERFIL ESPECIFICO DE UN RECLUTADOR / EMPRESA (Solo si está verificado corporativamente)
    @Transactional(readOnly = true)
    public RecruiterProfileResponseDTO getRecruiterProfileById(UUID recruiterId) {
        RecruiterProfile profile = recruiterRepository.findByIdAndIsVerifiedTrue(recruiterId)
                .orElseThrow(() -> new RuntimeException("HTTP 404: Perfil de empresa no encontrado o pendiente de verificación"));

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
}
