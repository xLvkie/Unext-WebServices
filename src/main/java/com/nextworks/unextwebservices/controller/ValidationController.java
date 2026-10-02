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

@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    @PostMapping("/request")
    public ResponseEntity<ValidationResponseDTO> requestValidation(
            Authentication authentication,
            @Valid @RequestBody ValidationRequestDTO request) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(validationService.requestValidation(user.getEmail(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ValidationResponseDTO>> getMyValidations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(validationService.getMyValidations(user.getEmail()));
    }
}
