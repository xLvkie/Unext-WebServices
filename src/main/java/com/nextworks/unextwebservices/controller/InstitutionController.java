package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.*;
import com.nextworks.unextwebservices.entity.AgreementStatus;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.ValidationStatus;
import com.nextworks.unextwebservices.service.InstitutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/institution")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/students/pending")
    public ResponseEntity<List<PendingStudentResponseDTO>> getPendingStudents(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.getPendingStudents(user.getEmail()));
        /*
        Retorna todos los estudiantes pendientes de revisión con respecto a la asociación academica
        El user requiere uso del token
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @PatchMapping("/students/{postulantId}/verify")
    public ResponseEntity<String> updateStudentAssociation(
            Authentication authentication,
            @PathVariable UUID postulantId,
            @RequestParam ValidationStatus status) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.updateStudentAssociation(user.getEmail(), postulantId, status));
        /*
        Aprueba la asosiación academica, solo acepta el parametro verify
        El user requiere uso del token
        verify?status=APPROVED etc
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @PostMapping("/companies/{recruiterId}/endorse")
    public ResponseEntity<String> endorseCompany(
            Authentication authentication,
            @PathVariable UUID recruiterId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.endorseCompany(user.getEmail(), recruiterId));
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @DeleteMapping("/companies/{recruiterId}/endorse")
    public ResponseEntity<String> removeEndorsement(
            Authentication authentication,
            @PathVariable UUID recruiterId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.removeEndorsement(user.getEmail(), recruiterId));
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/companies/endorsed")
    public ResponseEntity<List<EndorsedCompanyResponseDTO>> getEndorsedCompanies(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.getEndorsedCompanies(user.getEmail()));
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/agreements")
    public ResponseEntity<List<AgreementResponseDTO>> getAgreements(
            Authentication authentication,
            @RequestParam(required = false) AgreementStatus status) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.getAgreements(user.getEmail(), status));
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @PatchMapping("/agreements/{agreementId}/status")
    public ResponseEntity<AgreementResponseDTO> updateAgreementStatus(
            Authentication authentication,
            @PathVariable UUID agreementId,
            @Valid @RequestBody AgreementUpdateRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.updateAgreementStatus(user.getEmail(), agreementId, request));
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponseDTO> getDashboardStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.getDashboardStats(user.getEmail()));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'INSTITUTION', 'RECRUITER')")
    @GetMapping("/directory")
    public ResponseEntity<List<InstitutionDirectoryResponseDTO>> getAllInstitutions() {
        return ResponseEntity.ok(institutionService.getAllInstitutions());
        /*
        Retorna un listado de todas las instituciones
        El user requiere uso del token
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/validations/pending")
    public ResponseEntity<List<PendingValidationResponseDTO>> getPendingSkillValidations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.getPendingSkillValidations(user.getEmail()));
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @PatchMapping("/validations/{validationId}/status")
    public ResponseEntity<String> updateSkillValidationStatus(
            Authentication authentication,
            @PathVariable UUID validationId,
            @Valid @RequestBody ValidationUpdateRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(institutionService.updateSkillValidationStatus(user.getEmail(), validationId, request));
    }
}
