package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.directory.InstitutionDirectoryResponseDTO;
import com.nextworks.unextwebservices.dto.directory.PostulantDirectoryResponseDTO;
import com.nextworks.unextwebservices.dto.directory.RecruiterDirectoryResponseDTO;
import com.nextworks.unextwebservices.dto.job.JobOfferResponseDTO;
import com.nextworks.unextwebservices.dto.profile.*;
import com.nextworks.unextwebservices.entity.*;
import com.nextworks.unextwebservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PostulantProfileRepository postulantRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final JobOfferRepository jobOfferRepository;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public String resolveDisplayName(User user) {
        return switch (user.getRole()) {
            case POSTULANT -> postulantRepository.findByUserId(user.getId())
                    .map(p -> (p.getFirstName() + " " + p.getLastName()).trim())
                    .orElse(user.getEmail());
            case RECRUITER -> recruiterRepository.findByUserId(user.getId())
                    .map(RecruiterProfile::getCompanyName)
                    .orElse(user.getEmail());
            case INSTITUTION -> institutionRepository.findByUserId(user.getId())
                    .map(InstitutionProfile::getName)
                    .orElse(user.getEmail());
        };
    }

    /* ====================================
    // Creación del perfil de los segmentos
    // ==================================== */
    @Transactional
    public String createPostulantProfile(PostulantProfileRequestDTO request, String email) {
        User user = findUserByEmail(email);

        if (Boolean.TRUE.equals(user.getIsProfileCompleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfil ya está completado");
        }

        PostulantProfile profile = PostulantProfile.builder()
                .user(user)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .headline(request.getHeadline())
                .bio(request.getBio())
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
        User user = findUserByEmail(email);

        if (Boolean.TRUE.equals(user.getIsProfileCompleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfil ya está completado");
        }

        String cleanRuc = request.getRuc() != null ? request.getRuc().trim() : "";
        if (!cleanRuc.matches("^(10|20)\\d{9}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El RUC debe tener 11 dígitos válidos y comenzar con 10 o 20.");
        }

        if (recruiterRepository.existsByRuc(cleanRuc)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una empresa registrada con este RUC.");
        }

        String otpCode = generateOtpCode();

        RecruiterProfile profile = RecruiterProfile.builder()
                .user(user)
                .companyName(request.getCompanyName().trim())
                .ruc(cleanRuc)
                .industry(request.getIndustry())
                .headline(request.getHeadline())
                .websiteUrl(request.getWebsiteUrl())
                .description(request.getDescription())
                .companyLogoUrl(request.getCompanyLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .bannerText(request.getBannerText())
                .isValidated(false)
                .isVerified(false)
                .verificationCode(otpCode)
                .build();

        recruiterRepository.save(profile);

        user.setIsProfileCompleted(true);
        userRepository.save(user);

        return "Perfil de reclutador creado. Cuenta pendiente de verificación corporativa. Código de prueba: " + otpCode;
    }

    @Transactional
    public String createInstitutionProfile(InstitutionProfileRequestDTO request, String email) {
        User user = findUserByEmail(email);

        if (Boolean.TRUE.equals(user.getIsProfileCompleted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfil ya está completado");
        }

        String cleanDomain = request.getDomain().trim().toLowerCase();
        if (institutionRepository.existsByDomain(cleanDomain)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este dominio institucional ya está registrado.");
        }

        String otpCode = generateOtpCode();

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
        User user = findUserByEmail(email);

        switch (user.getRole()) {
            case RECRUITER -> {
                RecruiterProfile profile = recruiterRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Primero debes completar los datos de tu perfil de empresa"));

                if (Boolean.TRUE.equals(profile.getIsVerified())) {
                    return "Tu cuenta corporativa ya se encuentra verificada";
                }

                if (profile.getVerificationCode() == null || !profile.getVerificationCode().equals(code.trim())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Código de verificación corporativo incorrecto");
                }

                profile.setIsVerified(true);
                profile.setVerificationCode(null);
                recruiterRepository.save(profile);
            }
            case INSTITUTION -> {
                InstitutionProfile profile = institutionRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Primero debes completar los datos de tu perfil institucional"));

                if (Boolean.TRUE.equals(profile.getIsVerified())) {
                    return "Tu cuenta institucional ya se encuentra verificada";
                }

                if (profile.getVerificationCode() == null || !profile.getVerificationCode().equals(code.trim())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Código de verificación institucional incorrecto");
                }

                profile.setIsVerified(true);
                profile.setVerificationCode(null);
                institutionRepository.save(profile);
            }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Rol no aplicable para verificación organizacional");
        }

        user.setIsEmailVerified(true);
        userRepository.save(user);

        return "Organización verificada exitosamente. Tu perfil ahora es visible públicamente.";
    }

    @Transactional
    public String regenerateOrganizationOtp(String email) {
        User user = findUserByEmail(email);
        String otpCode = generateOtpCode();

        switch (user.getRole()) {
            case RECRUITER -> {
                RecruiterProfile profile = findRecruiterByUserId(user.getId());
                profile.setVerificationCode(otpCode);
                recruiterRepository.save(profile);

                notificationService.createNotification(
                        user,
                        "🔐 Código de Verificación Corporativa: " + otpCode,
                        "Ingresa el código de seguridad " + otpCode + " en tu perfil para acreditar el RUC " + profile.getRuc() + "."
                );
            }
            case INSTITUTION -> {
                InstitutionProfile profile = findInstitutionByUserId(user.getId());
                profile.setVerificationCode(otpCode);
                institutionRepository.save(profile);

                notificationService.createNotification(
                        user,
                        "🔐 Código de Acreditación Institucional: " + otpCode,
                        "Ingresa el código de seguridad " + otpCode + " en tu perfil para validar el dominio oficial @" + profile.getDomain() + "."
                );
            }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Rol no aplicable para código organizacional");
        }

        return otpCode;
    }

    /* ============================================
    // Mostrar y editar datos del perfil postulante
    // ============================================ */
    @Transactional(readOnly = true)
    public PostulantProfileResponseDTO getMyPostulantProfile(String email) {
        User user = findUserByEmail(email);
        PostulantProfile profile = findPostulantByUserId(user.getId());
        return toPostulantProfileDTO(profile);
    }

    @Transactional
    public PostulantProfileResponseDTO updateMyPostulantProfile(String email, PostulantProfileUpdateDTO request) {
        User user = findUserByEmail(email);
        PostulantProfile profile = findPostulantByUserId(user.getId());

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getCareer() != null) profile.setCareer(request.getCareer());
        if (request.getCurrentCycle() != null) profile.setCurrentCycle(request.getCurrentCycle());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getBannerUrl() != null) profile.setBannerUrl(request.getBannerUrl());
        if (request.getBannerText() != null) profile.setBannerText(request.getBannerText());
        if (request.getCvUrl() != null) profile.setCvUrl(request.getCvUrl());
        if (request.getCvFileName() != null) profile.setCvFileName(request.getCvFileName());
        if (request.getCvFileSize() != null) profile.setCvFileSize(request.getCvFileSize());
        if (request.getHeadline() != null) profile.setHeadline(request.getHeadline());
        if (request.getBio() != null) profile.setBio(request.getBio());

        postulantRepository.save(profile);
        return toPostulantProfileDTO(profile);
    }

    /* ============================================
    // Mostrar y editar datos del perfil reclutador
    // ============================================ */
    @Transactional
    public RecruiterProfileResponseDTO getMyRecruiterProfile(String email) {
        User user = findUserByEmail(email);
        RecruiterProfile profile = findRecruiterByUserId(user.getId());

        if (!Boolean.TRUE.equals(profile.getIsVerified()) &&
                (profile.getVerificationCode() == null || profile.getVerificationCode().isBlank())) {
            profile.setVerificationCode(generateOtpCode());
            recruiterRepository.save(profile);
        }

        return toRecruiterProfileDTO(profile, true);
    }

    @Transactional
    public RecruiterProfileResponseDTO updateMyRecruiterProfile(String email, RecruiterProfileUpdateDTO request) {
        User user = findUserByEmail(email);
        RecruiterProfile profile = findRecruiterByUserId(user.getId());

        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            profile.setCompanyName(request.getCompanyName().trim());
        }
        if (request.getIndustry() != null) profile.setIndustry(request.getIndustry());
        if (request.getHeadline() != null) profile.setHeadline(request.getHeadline());
        if (request.getWebsiteUrl() != null) profile.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getDescription() != null) profile.setDescription(request.getDescription());
        if (request.getCompanyLogoUrl() != null) profile.setCompanyLogoUrl(request.getCompanyLogoUrl());
        if (request.getBannerUrl() != null) profile.setBannerUrl(request.getBannerUrl());
        if (request.getBannerText() != null) profile.setBannerText(request.getBannerText());

        recruiterRepository.save(profile);
        return getMyRecruiterProfile(email);
    }

    // LISTADO DE TODOS LOS RECLUTADORES
    @Transactional(readOnly = true)
    public List<RecruiterDirectoryResponseDTO> getAllRecruiters() {
        return recruiterRepository.findByIsVerifiedTrue().stream()
                .map(profile -> RecruiterDirectoryResponseDTO.builder()
                        .id(profile.getId())
                        .userId(profile.getUser().getId())
                        .companyName(profile.getCompanyName())
                        .industry(profile.getIndustry())
                        .companyLogoUrl(profile.getCompanyLogoUrl())
                        .build())
                .toList();
    }

    // PERFIL ESPECIFICO DE UN RECLUTADOR / EMPRESA (Solo si está verificado corporativamente)
    @Transactional(readOnly = true)
    public RecruiterProfileResponseDTO getRecruiterProfileById(UUID recruiterId) {
        RecruiterProfile profile = recruiterRepository.findByIdAndIsVerifiedTrue(recruiterId)
                .or(() -> recruiterRepository.findByUserId(recruiterId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Perfil de empresa no encontrado o pendiente de verificación"));

        return toRecruiterProfileDTO(profile, false);
    }

    /* ============================================
    // Mostrar y editar datos del perfil institucion
    // ============================================ */
    @Transactional
    public InstitutionProfileResponseDTO getMyInstitutionProfile(String email) {
        User user = findUserByEmail(email);
        InstitutionProfile profile = findInstitutionByUserId(user.getId());

        if (!Boolean.TRUE.equals(profile.getIsVerified()) &&
                (profile.getVerificationCode() == null || profile.getVerificationCode().isBlank())) {
            profile.setVerificationCode(generateOtpCode());
            institutionRepository.save(profile);
        }

        return toInstitutionProfileDTO(profile, true);
    }

    @Transactional
    public InstitutionProfileResponseDTO updateMyInstitutionProfile(String email, InstitutionProfileUpdateDTO request) {
        User user = findUserByEmail(email);
        InstitutionProfile profile = findInstitutionByUserId(user.getId());

        if (request.getName() != null && !request.getName().isBlank()) profile.setName(request.getName().trim());
        if (request.getDomain() != null && !request.getDomain().isBlank()) profile.setDomain(request.getDomain().trim().toLowerCase());
        if (request.getLogoUrl() != null) profile.setLogoUrl(request.getLogoUrl());
        if (request.getBannerUrl() != null) profile.setBannerUrl(request.getBannerUrl());
        if (request.getBannerText() != null) profile.setBannerText(request.getBannerText());
        if (request.getDescription() != null) profile.setDescription(request.getDescription());

        institutionRepository.save(profile);
        return getMyInstitutionProfile(email);
    }

    /* ===================================
    // Tabla de Habilidades del Postulante
    // =================================== */
    @Transactional
    public StudentSkillResponseDTO addSkill(String email, StudentSkillRequestDTO request) {
        User user = findUserByEmail(email);
        PostulantProfile profile = findPostulantByUserId(user.getId());

        if (studentSkillRepository.existsByPostulantProfileIdAndNameIgnoreCase(profile.getId(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes una habilidad técnica registrada con el nombre: " + request.getName());
        }

        boolean wantsTopSkill = Boolean.TRUE.equals(request.getIsTopSkill());
        if (wantsTopSkill) {
            long currentTopCount = studentSkillRepository.countByPostulantProfileIdAndIsTopSkillTrue(profile.getId());
            if (currentTopCount >= 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Solo puedes destacar un máximo de 3 aptitudes principales.");
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
        return toSkillDTO(skill);
    }

    // LISTADO DE HABILIDADES
    @Transactional(readOnly = true)
    public List<StudentSkillResponseDTO> getMySkills(String email) {
        User user = findUserByEmail(email);
        PostulantProfile profile = findPostulantByUserId(user.getId());

        return studentSkillRepository.findByPostulantProfileId(profile.getId()).stream()
                .map(this::toSkillDTO)
                .toList();
    }

    // ACTUALIZAR HABILIDAD TECNICA
    @Transactional
    public StudentSkillResponseDTO updateSkill(String email, UUID skillId, StudentSkillUpdateDTO request) {
        User user = findUserByEmail(email);
        PostulantProfile profile = findPostulantByUserId(user.getId());

        StudentSkill skill = studentSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habilidad no encontrada"));

        if (!skill.getPostulantProfile().getId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para editar esta habilidad");
        }

        if (request.getIsTopSkill() != null) {
            boolean markingAsTop = Boolean.TRUE.equals(request.getIsTopSkill());
            boolean wasAlreadyTop = Boolean.TRUE.equals(skill.getIsTopSkill());

            if (markingAsTop && !wasAlreadyTop) {
                long currentTopCount = studentSkillRepository.countByPostulantProfileIdAndIsTopSkillTrue(profile.getId());
                if (currentTopCount >= 3) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Ya tienes 3 aptitudes principales seleccionadas. Desmarca una primero.");
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
        return toSkillDTO(skill);
    }

    // ELIMINAR HABILIDAD TECNICA
    @Transactional
    public void deleteSkill(String email, UUID skillId) {
        User user = findUserByEmail(email);
        PostulantProfile profile = findPostulantByUserId(user.getId());

        StudentSkill skill = studentSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habilidad no encontrada"));

        // Validamos por seguridad que la habilidad pertenezca al estudiante que intenta borrarla
        if (!skill.getPostulantProfile().getId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta habilidad");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Perfil de postulante no encontrado o no verificado"));

        return toPostulantProfileDTO(profile);
    }

    // LISTADO DE TODOS LOS POSTULANTES (Solo perfiles verificados/completados)
    @Transactional(readOnly = true)
    public List<PostulantDirectoryResponseDTO> getAllPostulants() {
        return postulantRepository.findByIsVerifiedTrue().stream()
                .map(profile -> PostulantDirectoryResponseDTO.builder()
                        .id(profile.getId())
                        .userId(profile.getUser().getId())
                        .firstName(profile.getFirstName())
                        .lastName(profile.getLastName())
                        .career(profile.getCareer())
                        .headline(profile.getHeadline())
                        .avatarUrl(profile.getAvatarUrl())
                        .isInstitutionVerified(profile.getIsInstitutionVerified())
                        .build())
                .toList();
    }

    // LISTADO DE TODAS LAS INSTITUCIONES
    @Transactional(readOnly = true)
    public List<InstitutionDirectoryResponseDTO> getAllInstitutions() {
        return institutionRepository.findByIsVerifiedTrue().stream()
                .map(profile -> InstitutionDirectoryResponseDTO.builder()
                        .id(profile.getId())
                        .userId(profile.getUser().getId())
                        .name(profile.getName())
                        .domain(profile.getDomain())
                        .logoUrl(profile.getLogoUrl())
                        .description(profile.getDescription())
                        .build())
                .toList();
    }

    // PERFIL ESPECIFICO DE UNA INSTITUCION (Solo si está verificada institucionalmente)
    @Transactional(readOnly = true)
    public InstitutionProfileResponseDTO getInstitutionProfileById(UUID institutionId) {
        InstitutionProfile profile = institutionRepository.findByIdAndIsVerifiedTrue(institutionId)
                .or(() -> institutionRepository.findByUserId(institutionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Perfil de institución no encontrado o pendiente de verificación"));

        return toInstitutionProfileDTO(profile, false);
    }

    @Transactional(readOnly = true)
    public List<JobOfferResponseDTO> getPublicJobsByRecruiter(UUID recruiterId) {
        RecruiterProfile profile = recruiterRepository.findByIdAndIsVerifiedTrue(recruiterId)
                .or(() -> recruiterRepository.findByUserId(recruiterId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        return jobOfferRepository.findByRecruiterProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .filter(job -> Boolean.TRUE.equals(job.getIsActive()))
                .map(matchingService::toOfferDto)
                .toList();
    }

    /* ========================================
    // Helpers privados de búsqueda y mapeo DTO
    // ======================================== */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private PostulantProfile findPostulantByUserId(UUID userId) {
        return postulantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de postulante no encontrado"));
    }

    private RecruiterProfile findRecruiterByUserId(UUID userId) {
        return recruiterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de reclutador no encontrado"));
    }

    private InstitutionProfile findInstitutionByUserId(UUID userId) {
        return institutionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de institución no encontrado"));
    }

    private String generateOtpCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
    }

    private StudentSkillResponseDTO toSkillDTO(StudentSkill skill) {
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

    private PostulantProfileResponseDTO toPostulantProfileDTO(PostulantProfile profile) {
        List<StudentSkillResponseDTO> skillsDTO = studentSkillRepository.findByPostulantProfileId(profile.getId())
                .stream()
                .map(this::toSkillDTO)
                .toList();

        InstitutionProfile inst = profile.getInstitutionProfile();
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
                .institutionProfile(inst != null ? inst.getId() : null)
                .institutionName(inst != null ? inst.getName() : null)
                .institutionLogoUrl(inst != null ? inst.getLogoUrl() : null)
                .skills(skillsDTO)
                .build();
    }

    private RecruiterProfileResponseDTO toRecruiterProfileDTO(RecruiterProfile profile, boolean includeVerificationCode) {
        boolean isVerified = Boolean.TRUE.equals(profile.getIsVerified());
        return RecruiterProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .companyName(profile.getCompanyName())
                .ruc(profile.getRuc())
                .industry(profile.getIndustry())
                .headline(profile.getHeadline())
                .websiteUrl(profile.getWebsiteUrl())
                .description(profile.getDescription())
                .companyLogoUrl(profile.getCompanyLogoUrl())
                .bannerUrl(profile.getBannerUrl())
                .bannerText(profile.getBannerText())
                .isValidated(Boolean.TRUE.equals(profile.getIsValidated()))
                .isVerified(isVerified)
                .validationsCount(profile.getValidationsCount() != null ? profile.getValidationsCount() : 0)
                .pendingVerificationCode(includeVerificationCode && !isVerified ? profile.getVerificationCode() : null)
                .build();
    }

    private InstitutionProfileResponseDTO toInstitutionProfileDTO(InstitutionProfile profile, boolean includeVerificationCode) {
        boolean isVerified = Boolean.TRUE.equals(profile.getIsVerified());
        return InstitutionProfileResponseDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getName())
                .domain(profile.getDomain())
                .logoUrl(profile.getLogoUrl())
                .bannerUrl(profile.getBannerUrl())
                .bannerText(profile.getBannerText())
                .description(profile.getDescription())
                .isVerified(isVerified)
                .pendingVerificationCode(includeVerificationCode && !isVerified ? profile.getVerificationCode() : null)
                .build();
    }
}
