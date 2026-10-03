package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.ValidationRequestDTO;
import com.nextworks.unextwebservices.dto.ValidationResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.ValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.nextworks.unextwebservices.dto.ValidationStatusUpdateDTO;
@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor

// http://localhost:8080/

public class ValidationController {

    private final ValidationService validationService;

    @PostMapping("/request")
    public ResponseEntity<ValidationResponseDTO> requestValidation(
            Authentication authentication,
            @Valid @RequestBody ValidationRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(validationService.requestValidation(user.getEmail(), request));
        /*
        Para completar una solicitud de revision de habilidades se requiere completar los campos:
        institutionProfileId(!), knowledgeTitle(!) y evidenceUrl(!)

        # institutionProfileId no es el userId si no id (PK)
         */
    }

    @GetMapping("/me")
    public ResponseEntity<List<ValidationResponseDTO>> getMyValidations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(validationService.getMyValidations(user.getEmail()));
        /*
        Se requiere uso del bearer mediante el token del usuario
         */
    }

    @GetMapping("/institution")
    public ResponseEntity<List<ValidationResponseDTO>> getInstitutionValidations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(validationService.getInstitutionValidations(user.getEmail()));
        /*
        Endpoint para que la institución pueda ver todas las solicitudes recibidas
         */
    }

    @PatchMapping("/{validationId}/status")
    public ResponseEntity<ValidationResponseDTO> updateValidationStatus(
            Authentication authentication,
            @PathVariable UUID validationId,
            @Valid @RequestBody ValidationStatusUpdateDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(validationService.updateValidationStatus(user.getEmail(), validationId, request));
        /*
        Endpoint para que la institución apruebe (APPROVED) o rechace (REJECTED) una solicitud de validación
         */
    }
}
