package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.validation.AcademicVerificationRequestDTO;
import com.nextworks.unextwebservices.dto.validation.ValidationRequestDTO;
import com.nextworks.unextwebservices.dto.validation.ValidationResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.ValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "5. Validaciones Estudiantiles", description = "Solicitudes de convalidación académica por parte de postulantes")
@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    @PreAuthorize("hasAuthority('POSTULANT')")
    @PostMapping("/request")
    public ResponseEntity<ValidationResponseDTO> requestValidation(
            Authentication authentication,
            @Valid @RequestBody ValidationRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(validationService.requestValidation(user.getEmail(), request));
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @GetMapping("/me")
    public ResponseEntity<List<ValidationResponseDTO>> getMyValidations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(validationService.getMyValidations(user.getEmail()));
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @PostMapping("/me/academic-verification")
    public ResponseEntity<String> requestAcademicVerification(
            Authentication authentication,
            @Valid @RequestBody AcademicVerificationRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(validationService.requestAcademicVerification(user.getEmail(), request));
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @PatchMapping("/me/institution/{institutionId}")
    public ResponseEntity<String> linkInstitution(
            Authentication authentication,
            @PathVariable UUID institutionId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(validationService.linkInstitution(user.getEmail(), institutionId));
    }
}