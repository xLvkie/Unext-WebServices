package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.*;

import com.nextworks.unextwebservices.entity.User;

import com.nextworks.unextwebservices.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@Tag(name = "2. Perfiles y Habilidades", description = "Gestión de perfiles de usuario y habilidades estudiantiles")
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /* ======================
    COMPLETAR PERFIL SEGMENTO
    // ====================== */
    @PostMapping("/postulant/{userId}")
    public ResponseEntity<String> createPostulantProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody PostulantProfileRequestDTO request) {

        return ResponseEntity.ok(profileService.createPostulantProfile(userId, request));
        /*
        Para completar el perfil postulante se requiere completar los campos: firstName(!), lastName(!),
        studentCode, career y currentCycle.
         */
    }

    @PostMapping("/recruiter/{userId}")
    public ResponseEntity<String> createRecruiterProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody RecruiterProfileRequestDTO request) {

        return ResponseEntity.ok(profileService.createRecruiterProfile(userId, request));
        /*
        Para completar el perfil reclutador se requiere completar los campos: companyName(!), ruc(!),
        industry y description.
         */
    }

    @PostMapping("/institution/{userId}")
    public ResponseEntity<String> createInstitutionProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody InstitutionProfileRequestDTO request) {

        return ResponseEntity.ok(profileService.createInstitutionProfile(userId, request));
        /*
        Para completar el perfil institucional se requiere completar los campos: institutionName(!) y domain(!)
         */
    }

    /* ========================
    ACTUALIZAR Y OBTENER PERFIL
    // ======================== */
    @GetMapping("/postulant/me")
    public ResponseEntity<PostulantProfileResponseDTO> getMyProfileStudent(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.getMyPostulantProfile(email));
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PutMapping("/postulant/me")
    public ResponseEntity<PostulantProfileResponseDTO> updateMyProfileStudent(
            Authentication authentication,
            @Valid @RequestBody PostulantProfileUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.updateMyPostulantProfile(email, request));
        /*
        Campos modificables: career, currentCycle, cvUrl, headline y bio
         */
    }

    @GetMapping("/recruiter/me")
    public ResponseEntity<RecruiterProfileResponseDTO> getMyProfileRecruiter(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.getMyRecruiterProfile(email));
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PutMapping("/recruiter/me")
    public ResponseEntity<RecruiterProfileResponseDTO> updateMyProfileRecruiter(
            Authentication authentication,
            @Valid @RequestBody RecruiterProfileUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.updateMyRecruiterProfile(email, request));
        /*
        Campos modificables: companyName, industry y description
         */
    }

    @GetMapping("/institution/me")
    public ResponseEntity<InstitutionProfileResponseDTO> getMyProfileInstitution(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.getMyInstitutionProfile(email));
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PutMapping("/institution/me")
    public ResponseEntity<InstitutionProfileResponseDTO> updateMyProfileInstitution(
            Authentication authentication,
            @Valid @RequestBody InstitutionProfileUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.updateMyInstitutionProfile(email, request));
        /*
        Campos modificables: name, domain, logoUrl y description
         */
    }

    /* ========================
    HABILIDADES DEL POSTULANTE
    // ======================== */
    @PostMapping("/postulant/me/skills")
    public ResponseEntity<StudentSkillResponseDTO> addSkill(
            Authentication authentication,
            @Valid @RequestBody StudentSkillRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(profileService.addSkill(user.getEmail(), request));
        /*
        Para completar una habilidad tecnica se requiere completar los campos: name(!) y masteryLevel(!)
         */
    }

    @GetMapping("/postulant/me/skills")
    public ResponseEntity<List<StudentSkillResponseDTO>> getMySkills(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(profileService.getMySkills(user.getEmail()));
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PutMapping("/postulant/me/skills/{skillId}")
    public ResponseEntity<StudentSkillResponseDTO> updateSkill(
            Authentication authentication,
            @PathVariable UUID skillId,
            @Valid @RequestBody StudentSkillUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(profileService.updateSkill(user.getEmail(), skillId, request));
        /*
        Campos modificables: name y masteryLevel
         */
    }

    @DeleteMapping("/postulant/me/skills/{skillId}")
    public ResponseEntity<Void> deleteSkill(
            Authentication authentication,
            @PathVariable UUID skillId) {

        User user = (User) authentication.getPrincipal();
        profileService.deleteSkill(user.getEmail(), skillId);
        return ResponseEntity.noContent().build();
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    /* =======================================
    // CERTIFICACIÓN POR PARTE DE INSTITUCIONES
    // ======================================= */
    @PatchMapping("/institution/skills/{skillId}/certify")
    public ResponseEntity<StudentSkillResponseDTO> certifyStudentSkill(
            Authentication authentication,
            @PathVariable UUID skillId) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(profileService.certifyStudentSkill(user.getEmail(), skillId));
        /*
        Endpoint para que una institución valide/certifique la habilidad técnica de un estudiante.
        Cambia isValidatedByInstitution a true.
         */
    }
}