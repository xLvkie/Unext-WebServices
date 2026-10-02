package com.nextworks.unextwebservices.dto;

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

    // Obligatorio por ahora, ya que estamos en el contexto de empleabilidad (no se implementan aún los demas segmentos)
    @NotNull(message = "El ID de la postulación es obligatorio para este tipo de chat")
    private UUID jobApplicationId;
}