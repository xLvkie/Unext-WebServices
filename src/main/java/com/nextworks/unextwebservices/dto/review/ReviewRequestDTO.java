package com.nextworks.unextwebservices.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequestDTO {
    @NotNull(message = "El convenio es obligatorio")
    private UUID agreementId;

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer stars;

    @NotBlank(message = "La reseña es obligatoria")
    private String comment;

    private Boolean publish;
}
