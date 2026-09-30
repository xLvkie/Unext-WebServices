package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.InstitutionProfileRequestDTO;
import com.nextworks.unextwebservices.dto.PostulantProfileRequestDTO;
import com.nextworks.unextwebservices.dto.RecruiterProfileRequestDTO;

import com.nextworks.unextwebservices.dto.PostulantProfileUpdateDTO;
import com.nextworks.unextwebservices.dto.PostulantProfileResponseDTO;

import com.nextworks.unextwebservices.entity.User;

import com.nextworks.unextwebservices.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @GetMapping("/postulant/me")
    public ResponseEntity<PostulantProfileResponseDTO> getMyProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.getMyPostulantProfile(email));
    }

    @PutMapping("/postulant/me")
    public ResponseEntity<PostulantProfileResponseDTO> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody PostulantProfileUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        String email = user.getEmail();

        return ResponseEntity.ok(profileService.updateMyPostulantProfile(email, request));
    }
}