package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.notification.NotificationResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "10. Notificaciones", description = "Gestión y lectura de alertas del sistema")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'INSTITUTION', 'RECRUITER')")
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(notificationService.getMyNotifications(user.getEmail()));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'INSTITUTION', 'RECRUITER')")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            Authentication authentication,
            @PathVariable UUID notificationId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(notificationService.markAsRead(user.getEmail(), notificationId));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'INSTITUTION', 'RECRUITER')")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        notificationService.markAllAsRead(user.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'INSTITUTION', 'RECRUITER')")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            Authentication authentication,
            @PathVariable UUID notificationId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        notificationService.deleteNotification(user.getEmail(), notificationId);
        return ResponseEntity.noContent().build();
    }
}