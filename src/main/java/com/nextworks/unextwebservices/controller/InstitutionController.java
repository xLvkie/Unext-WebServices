package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.*;
import com.nextworks.unextwebservices.entity.AgreementStatus;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.InstitutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/institution")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping("/students/pending")
    public ResponseEntity<List<PendingStudentResponseDTO>> getPendingStudents(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.getPendingStudents(user.getEmail()));
        /*
        Retorna todos los estudiantes pendientes de revisión con respecto a la asociación academica
        El user requiere uso del token
         */
    }

    @PatchMapping("/students/{postulantId}/verify")
    public ResponseEntity<String> verifyStudentProfile(
            Authentication authentication,
            @PathVariable UUID postulantId) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.verifyStudentProfile(user.getEmail(), postulantId));
        /*
        Aprueba la asosiación academica, solo acepta el parametro verify
        El user requiere uso del token
         */
    }

    @PostMapping("/companies/{recruiterId}/endorse")
    public ResponseEntity<String> endorseCompany(
            Authentication authentication,
            @PathVariable UUID recruiterId) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.endorseCompany(user.getEmail(), recruiterId));
    }

    @DeleteMapping("/companies/{recruiterId}/endorse")
    public ResponseEntity<String> removeEndorsement(
            Authentication authentication,
            @PathVariable UUID recruiterId) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.removeEndorsement(user.getEmail(), recruiterId));
    }

    @GetMapping("/companies/endorsed")
    public ResponseEntity<List<EndorsedCompanyResponseDTO>> getEndorsedCompanies(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.getEndorsedCompanies(user.getEmail()));
    }

    @GetMapping("/agreements")
    public ResponseEntity<List<AgreementResponseDTO>> getAgreements(
            Authentication authentication,
            @RequestParam(required = false) AgreementStatus status) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.getAgreements(user.getEmail(), status));
    }

    @PatchMapping("/agreements/{agreementId}/status")
    public ResponseEntity<AgreementResponseDTO> updateAgreementStatus(
            Authentication authentication,
            @PathVariable UUID agreementId,
            @Valid @RequestBody AgreementUpdateRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.updateAgreementStatus(user.getEmail(), agreementId, request));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponseDTO> getDashboardStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.getDashboardStats(user.getEmail()));
    }

    @GetMapping("/directory")
    public ResponseEntity<List<InstitutionDirectoryResponseDTO>> getAllInstitutions() {
        return ResponseEntity.ok(institutionService.getAllInstitutions());
        /*
        Retorna un listado de todas las instituciones
        El user requiere uso del token
         */
    }

    @GetMapping("/validations/pending")
    public ResponseEntity<List<PendingValidationResponseDTO>> getPendingSkillValidations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.getPendingSkillValidations(user.getEmail()));
    }

    @PatchMapping("/validations/{validationId}/status")
    public ResponseEntity<String> updateSkillValidationStatus(
            Authentication authentication,
            @PathVariable UUID validationId,
            @Valid @RequestBody ValidationUpdateRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(institutionService.updateSkillValidationStatus(user.getEmail(), validationId, request));
    }
}
