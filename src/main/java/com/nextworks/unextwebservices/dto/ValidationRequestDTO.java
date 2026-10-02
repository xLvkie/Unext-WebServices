package com.nextworks.unextwebservices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ValidationRequestDTO {
    @NotNull(message = "El ID de la institución es obligatorio")
    private UUID institutionProfileId;

    @NotBlank(message = "El título del conocimiento a validar es obligatorio")
    private String knowledgeTitle;

    @NotBlank(message = "La URL de evidencia (certificado/proyecto) es obligatoria")
    private String evidenceUrl;
}
