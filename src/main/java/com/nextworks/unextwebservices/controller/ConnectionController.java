package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.connection.ConnectionStatusResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.ConnectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "12. Red y Conexiones", description = "Gestión de conexiones profesionales entre usuarios")
@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @GetMapping("/status/{targetUserId}")
    public ResponseEntity<ConnectionStatusResponseDTO> getStatusWithUser(
            Authentication authentication,
            @PathVariable UUID targetUserId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(connectionService.getStatusWithUser(user.getEmail(), targetUserId));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/request/{targetUserId}")
    public ResponseEntity<ConnectionStatusResponseDTO> sendRequest(
            Authentication authentication,
            @PathVariable UUID targetUserId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(connectionService.sendConnectionRequest(user.getEmail(), targetUserId));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/{connectionId}/accept")
    public ResponseEntity<ConnectionStatusResponseDTO> acceptRequest(
            Authentication authentication,
            @PathVariable UUID connectionId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(connectionService.acceptConnectionRequest(user.getEmail(), connectionId));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @DeleteMapping("/user/{targetUserId}")
    public ResponseEntity<Void> removeOrCancelConnection(
            Authentication authentication,
            @PathVariable UUID targetUserId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        connectionService.removeOrCancelConnection(user.getEmail(), targetUserId);
        return ResponseEntity.noContent().build();
    }
}