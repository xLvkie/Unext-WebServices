package com.nextworks.unextwebservices.dto.job;

import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class JobOfferRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @NotBlank(message = "Los requisitos son obligatorios")
    private String requirements;

    @NotEmpty(message = "Debes agregar al menos una habilidad requerida")
    private List<String> requiredSkills;

    private String location;

    @NotNull(message = "La modalidad es obligatoria")
    private JobModality modality;

    @NotNull(message = "El nivel de experiencia es obligatorio")
    private ExperienceLevel experienceLevel;

    private BigDecimal minSalary;
    private BigDecimal maxSalary;
}