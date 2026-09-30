package com.nextworks.unextwebservices.security;

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
        // 1. Validar que el correo no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El correo ya está registrado en el sistema");
        }

        // 2. Crear la entidad User mapeando los datos del DTO
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .authProvider("LOCAL")
                .isProfileCompleted(false)
                .isEmailVerified(false)
                .build();

        // 3. Guardar en PostgreSQL
        userRepository.save(user);

        // 4. Generar Token JWT
        String token = jwtUtil.generateToken(user.getEmail());

        // 5. Retornar el DTO de respuesta
        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole())
                .isProfileCompleted(user.getIsProfileCompleted())
                .build();
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        // 1. Buscar al usuario por correo
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        // 2. Verificar que la contraseña encriptada coincida
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        // 3. Generar Token JWT
        String token = jwtUtil.generateToken(user.getEmail());

        // 4. Retornar respuesta
        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole())
                .isProfileCompleted(user.getIsProfileCompleted())
                .build();
    }
}
