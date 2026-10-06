package com.nextworks.unextwebservices.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class InstitutionProfileRequestDTO {
    @NotBlank(message = "El nombre de la institución es obligatorio")
    private String institutionName;

    @NotBlank(message = "El dominio es obligatorio")
    private String domain;
}
