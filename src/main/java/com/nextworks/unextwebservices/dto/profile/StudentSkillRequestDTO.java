package com.nextworks.unextwebservices.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentSkillRequestDTO {
    @NotBlank(message = "El nombre de la habilidad es obligatorio")
    private String name;

    @NotBlank(message = "El nivel de dominio es obligatorio")
    private String masteryLevel;
}