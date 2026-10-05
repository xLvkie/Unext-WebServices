package com.nextworks.unextwebservices.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidationUpdateRequestDTO {

    @NotNull(message = "El estado es obligatorio")
    private String status;
    private String observations;
}