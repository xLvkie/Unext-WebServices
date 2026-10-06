package com.nextworks.unextwebservices.dto.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ValidationRequestDTO {
    @NotNull(message = "El ID de la institución es obligatorio")
    private UUID institutionProfileId;

    @NotNull(message = "El ID de la habilidad técnica es obligatorio")
    private UUID technicalSkillId;

    @NotBlank(message = "La URL de evidencia (certificado/proyecto) es obligatoria")
    private String evidenceUrl;
}
