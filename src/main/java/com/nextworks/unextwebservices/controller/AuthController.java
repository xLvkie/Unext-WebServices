package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.auth.AuthResponseDTO;
import com.nextworks.unextwebservices.dto.auth.LoginRequestDTO;
import com.nextworks.unextwebservices.dto.auth.RegisterRequestDTO;
import com.nextworks.unextwebservices.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Autenticación", description = "Endpoints de registro e inicio de sesión")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
        /*
        Para completar el registro se pide rellenar los campos email, password y role.
        role solo permite: [INSTITUTION, RECRUITER, POSTULANT]
         */
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
        /*
        Para completar el login se pide completar los campos de email y password
         */
    }
}
