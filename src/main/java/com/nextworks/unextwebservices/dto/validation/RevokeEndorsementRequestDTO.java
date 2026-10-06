package com.nextworks.unextwebservices.dto.validation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RevokeEndorsementRequestDTO {
    @NotBlank(message = "Debe ingresar un motivo para revocar la insignia")
    private String reason;
}
