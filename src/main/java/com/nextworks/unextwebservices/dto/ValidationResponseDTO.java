package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.ValidationStatus;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ValidationResponseDTO {
    private UUID id;
    private String institutionName; // Enviamos el nombre de la U para que el frontend lo muestre bonito
    private String knowledgeTitle;
    private String evidenceUrl;
    private ValidationStatus status;
}
