package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.ExperienceLevel;
import com.nextworks.unextwebservices.entity.JobModality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class JobOfferRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "La descripción es obligatoria")
    private String description;

    @NotBlank(message = "Los requisitos son obligatorios")
    private String requirements;

    private String location;

    @NotNull(message = "La modalidad es obligatoria")
    private JobModality modality;

    @NotNull(message = "El nivel de experiencia es obligatorio")
    private ExperienceLevel experienceLevel;

    private BigDecimal minSalary;
    private BigDecimal maxSalary;
}