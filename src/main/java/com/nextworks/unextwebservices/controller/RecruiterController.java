package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.ApplicationResponseDTO;
import com.nextworks.unextwebservices.dto.JobOfferRequestDTO;
import com.nextworks.unextwebservices.dto.JobOfferResponseDTO;
import com.nextworks.unextwebservices.entity.ApplicationStatus;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.RecruiterService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "4. Panel Reclutador", description = "Gestión de ofertas laborales y revisión de candidatos")
@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    private final RecruiterService recruiterService;

    // Crear nueva vacante
    @PostMapping("/jobs")
    public ResponseEntity<JobOfferResponseDTO> createJobOffer(
            Authentication authentication,
            @Valid @RequestBody JobOfferRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(recruiterService.createJobOffer(user.getEmail(), request));
    }

    // Cambiar estado de postulación (y disparar notificación)
    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<String> updateApplicationStatus(
            Authentication authentication,
            @PathVariable UUID applicationId,
            @RequestParam ApplicationStatus status) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(recruiterService.updateApplicationStatus(user.getEmail(), applicationId, status));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobOfferResponseDTO>> getMyJobOffers(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(recruiterService.getMyJobOffers(user.getEmail()));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponseDTO>> getApplicationsForJob(
            Authentication authentication,
            @PathVariable UUID jobId) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(recruiterService.getApplicationsForJob(user.getEmail(), jobId));
    }

    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<JobOfferResponseDTO> updateJobOffer(
            Authentication authentication,
            @PathVariable UUID jobId,
            @Valid @RequestBody JobOfferRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(recruiterService.updateJobOffer(user.getEmail(), jobId, request));
    }
}
