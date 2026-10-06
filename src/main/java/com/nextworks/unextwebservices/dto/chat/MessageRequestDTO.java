package com.nextworks.unextwebservices.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class MessageRequestDTO {

    @NotNull(message = "El ID del destinatario es obligatorio")
    private UUID receiverId;

    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    private String content;

    private UUID jobApplicationId;
}