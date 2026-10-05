package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.*;

import com.nextworks.unextwebservices.entity.User;

import com.nextworks.unextwebservices.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor

// http://localhost:8080/
// De ahora en adelante es obligatorio el uso del bearer token

public class ProfileController {

    private final ProfileService profileService;

    /* ======================
    COMPLETAR PERFIL SEGMENTO
    // ====================== */
    @PreAuthorize("hasAuthority('POSTULANT')")
    @PostMapping("/postulant/me")
    public ResponseEntity<String> createPostulantProfile(
            Authentication authentication,
            @Valid @RequestBody PostulantProfileRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.createPostulantProfile(request, user.getEmail()));
        /*
        Para completar el perfil postulante se requiere completar los campos: firstName(!), lastName(!),
        studentCode, career y currentCycle.
         */
    }

    @PreAuthorize("hasAuthority('RECRUITER')")
    @PostMapping("/recruiter/me")
    public ResponseEntity<String> createRecruiterProfile(
            Authentication authentication,
            @Valid @RequestBody RecruiterProfileRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.createRecruiterProfile(request, user.getEmail()));
        /*
        Para completar el perfil reclutador se requiere completar los campos: companyName(!), ruc(!),
        industry y description.
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @PostMapping("/institution/me")
    public ResponseEntity<String> createInstitutionProfile(
            Authentication authentication,
            @Valid @RequestBody InstitutionProfileRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.createInstitutionProfile(request, user.getEmail()));
        /*
        Para completar el perfil institucional se requiere completar los campos: institutionName(!) y domain(!)
         */
    }

    /* ========================
    ACTUALIZAR Y OBTENER PERFIL
    // ======================== */
    @PreAuthorize("hasAuthority('POSTULANT')")
    @GetMapping("/postulant/me")
    public ResponseEntity<PostulantProfileResponseDTO> getMyProfileStudent(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;

        return ResponseEntity.ok(profileService.getMyPostulantProfile(user.getEmail()));
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @PutMapping("/postulant/me")
    public ResponseEntity<PostulantProfileResponseDTO> updateMyProfileStudent(
            Authentication authentication,
            @Valid @RequestBody PostulantProfileUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.updateMyPostulantProfile(user.getEmail(), request));
        /*
        Campos modificables: career, currentCycle, cvUrl, headline y bio
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PreAuthorize("hasAuthority('RECRUITER')")
    @GetMapping("/recruiter/me")
    public ResponseEntity<RecruiterProfileResponseDTO> getMyProfileRecruiter(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.getMyRecruiterProfile(user.getEmail()));
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PreAuthorize("hasAuthority('RECRUITER')")
    @PutMapping("/recruiter/me")
    public ResponseEntity<RecruiterProfileResponseDTO> updateMyProfileRecruiter(
            Authentication authentication,
            @Valid @RequestBody RecruiterProfileUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.updateMyRecruiterProfile(user.getEmail(), request));
        /*
        Campos modificables: companyName, industry y description
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/institution/me")
    public ResponseEntity<InstitutionProfileResponseDTO> getMyProfileInstitution(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.getMyInstitutionProfile(user.getEmail()));
        /*
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @PutMapping("/institution/me")
    public ResponseEntity<InstitutionProfileResponseDTO> updateMyProfileInstitution(
            Authentication authentication,
            @Valid @RequestBody InstitutionProfileUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.updateMyInstitutionProfile(user.getEmail(), request));
        /*
        Campos modificables: name, domain, logoUrl y description
        Se debe hacer uso del bearer token del usuario
         */
    }

    /* ========================
    HABILIDADES DEL POSTULANTE
    // ======================== */
    @PreAuthorize("hasAuthority('POSTULANT')")
    @PostMapping("/postulant/me/skills")
    public ResponseEntity<StudentSkillResponseDTO> addSkill(
            Authentication authentication,
            @Valid @RequestBody StudentSkillRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.addSkill(user.getEmail(), request));
        /*
        Para completar una habilidad tecnica se requiere completar los campos: name(!) y masteryLevel(!)
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @GetMapping("/postulant/me/skills")
    public ResponseEntity<List<StudentSkillResponseDTO>> getMySkills(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.getMySkills(user.getEmail()));
        /*
        Devuelve un listado de todas las habilidades
        Se debe hacer uso del bearer token del usuario
         */
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @PutMapping("/postulant/me/skills/{skillId}")
    public ResponseEntity<StudentSkillResponseDTO> updateSkill(
            Authentication authentication,
            @PathVariable UUID skillId,
            @Valid @RequestBody StudentSkillUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(profileService.updateSkill(user.getEmail(), skillId, request));
        /*
        Campos modificables: name y masteryLevel
        Se debe hacer uso del bearer token del User
         */
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @DeleteMapping("/postulant/me/skills/{skillId}")
    public ResponseEntity<Void> deleteSkill(
            Authentication authentication,
            @PathVariable UUID skillId) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        profileService.deleteSkill(user.getEmail(), skillId);
        return ResponseEntity.noContent().build();
        /*
        Elimina un habilidad mediante el <ID DE LA HABILIDAD>
        Se debe hacer uso del bearer token del usuario
         */
    }
}