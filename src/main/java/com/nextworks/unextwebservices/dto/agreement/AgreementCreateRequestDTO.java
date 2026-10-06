package com.nextworks.unextwebservices.dto.agreement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AgreementCreateRequestDTO {

    @NotNull(message = "El ID del postulante es obligatorio")
    private UUID postulantId;

    // Opcional: si se indica, el convenio queda ligado a esa postulación y se valida que la vacante sea TRAINEE
    private UUID jobApplicationId;

    @NotBlank(message = "El título del convenio es obligatorio")
    private String title;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    @NotNull(message = "Las horas semanales son obligatorias")
    private Integer weeklyHours;

    @NotBlank(message = "El enlace al documento es obligatorio")
    private String documentUrl;

    private String observations;
}