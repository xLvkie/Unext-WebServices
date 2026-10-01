package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.AuthResponseDTO;
import com.nextworks.unextwebservices.dto.LoginRequestDTO;
import com.nextworks.unextwebservices.dto.RegisterRequestDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.UserRepository;
import com.nextworks.unextwebservices.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El correo ya está registrado en el sistema");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .authProvider("LOCAL")
                .isProfileCompleted(false)
                .isEmailVerified(false)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole())
                .isProfileCompleted(user.getIsProfileCompleted())
                .build();
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole())
                .isProfileCompleted(user.getIsProfileCompleted())
                .build();
    }
}
