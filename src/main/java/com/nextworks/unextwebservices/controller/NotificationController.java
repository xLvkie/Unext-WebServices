package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.NotificationResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "7. Notificaciones", description = "Gestión y lectura de alertas del sistema")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getMyNotifications(user.getEmail()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            Authentication authentication,
            @PathVariable UUID notificationId) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.markAsRead(user.getEmail(), notificationId));
    }
}