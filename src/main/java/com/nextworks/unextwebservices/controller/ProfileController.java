package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.InstitutionProfileRequestDTO;
import com.nextworks.unextwebservices.dto.PostulantProfileRequestDTO;
import com.nextworks.unextwebservices.dto.RecruiterProfileRequestDTO;
import com.nextworks.unextwebservices.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/postulant/{userId}")
    public ResponseEntity<String> createPostulantProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody PostulantProfileRequestDTO request) {

        return ResponseEntity.ok(profileService.createPostulantProfile(userId, request));
    }

    @PostMapping("/recruiter/{userId}")
    public ResponseEntity<String> createRecruiterProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody RecruiterProfileRequestDTO request) {

        return ResponseEntity.ok(profileService.createRecruiterProfile(userId, request));
    }

    @PostMapping("/institution/{userId}")
    public ResponseEntity<String> createInstitutionProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody InstitutionProfileRequestDTO request) {

        return ResponseEntity.ok(profileService.createInstitutionProfile(userId, request));
    }
}