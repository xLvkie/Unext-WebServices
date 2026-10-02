package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.ChatContext;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageResponseDTO {
    private UUID id;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private LocalDateTime createdAt;
    private Boolean isRead;
    private ChatContext contextType;
}