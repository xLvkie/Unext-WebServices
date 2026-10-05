package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.ApplicationResponseDTO;
import com.nextworks.unextwebservices.dto.JobOfferRequestDTO;
import com.nextworks.unextwebservices.dto.JobOfferResponseDTO;
import com.nextworks.unextwebservices.dto.RecruiterDirectoryResponseDTO;
import com.nextworks.unextwebservices.entity.ApplicationStatus;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    private final RecruiterService recruiterService;

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'INSTITUTION', 'RECRUITER')")
    @GetMapping("/directory")
    public ResponseEntity<List<RecruiterDirectoryResponseDTO>> getAllRecruiters() {
        return ResponseEntity.ok(recruiterService.getAllRecruiters());
        /*
        Retorna un listado de todos las empresas
        Se quiere que el User use su token
         */
    }

    // Crear nueva vacante
    @PreAuthorize("hasAuthority('RECRUITER')")
    @PostMapping("/jobs")
    public ResponseEntity<JobOfferResponseDTO> createJobOffer(
            Authentication authentication,
            @Valid @RequestBody JobOfferRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(recruiterService.createJobOffer(user.getEmail(), request));
        /*
        Para completar la creación de la vacante se requieren rellenar los campos: title (!), description (!), requirements (!),
        location, modality (!), experienceLevel (!), minSalary, maxSalary
        Se quiere que el User use su token
         */
    }

    // Cambiar estado de postulación (y disparar notificación)
    @PreAuthorize("hasAuthority('RECRUITER')")
    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<String> updateApplicationStatus(
            Authentication authentication,
            @PathVariable UUID applicationId,
            @RequestParam ApplicationStatus status) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(recruiterService.updateApplicationStatus(user.getEmail(), applicationId, status));
        /*
        Cambia el estado de la aplicación de un postulante
        Se quiere que el User use su token
         */
    }

    @PreAuthorize("hasAuthority('RECRUITER')")
    @GetMapping("/jobs")
    public ResponseEntity<List<JobOfferResponseDTO>> getMyJobOffers(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(recruiterService.getMyJobOffers(user.getEmail()));
        /*
        Retorna un listado de todos las vacantes de la empresa
        Se quiere que el User use su token
         */
    }

    @PreAuthorize("hasAuthority('RECRUITER')")
    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponseDTO>> getApplicationsForJob(
            Authentication authentication,
            @PathVariable UUID jobId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(recruiterService.getApplicationsForJob(user.getEmail(), jobId));
        /*
        Retorna un listado de todos las aplicaciones de los postulantes a una vacante de la empresa
        Se quiere que el User use su token
         */
    }

    @PreAuthorize("hasAuthority('RECRUITER')")
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<JobOfferResponseDTO> updateJobOffer(
            Authentication authentication,
            @PathVariable UUID jobId,
            @Valid @RequestBody JobOfferRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(recruiterService.updateJobOffer(user.getEmail(), jobId, request));
        /*
        Actualiza las caracteristicas de una vacante con todos estos campos: title (!), description (!), requirements (!),
        location, modality (!), experienceLevel (!), minSalary, maxSalary
        Se quiere que el User use su token
         */
    }
}
