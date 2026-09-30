package com.nextworks.unextwebservices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class InstitutionProfileRequestDTO {
    @NotBlank(message = "El nombre de la institución es obligatorio")
    private String institutionName;

    @NotBlank(message = "El codigo es obligatorio")
    private String institutionalCode;

    private String contactEmail;
    private String representativeName;
}
