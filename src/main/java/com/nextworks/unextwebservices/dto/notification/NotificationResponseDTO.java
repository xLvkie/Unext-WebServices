package com.nextworks.unextwebservices.dto.notification;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponseDTO {
    private UUID id;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
