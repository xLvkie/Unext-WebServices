package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.notification.NotificationResponseDTO;
import com.nextworks.unextwebservices.entity.Notification;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.NotificationRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(User user, String title, String content) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public NotificationResponseDTO markAsRead(String email, UUID notificationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para modificar esta notificación");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        return mapToDTO(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(user.getId());
        if (!unread.isEmpty()) {
            unread.forEach(n -> n.setIsRead(true));
            notificationRepository.saveAll(unread);
        }
    }

    @Transactional
    public void deleteNotification(String email, UUID notificationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar esta notificación");
        }

        notificationRepository.delete(notification);
    }

    private NotificationResponseDTO mapToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(Boolean.TRUE.equals(notification.getIsRead()))
                .createdAt(notification.getCreatedAt())
                .build();
    }
}