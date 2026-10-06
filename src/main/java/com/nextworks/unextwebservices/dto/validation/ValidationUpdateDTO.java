package com.nextworks.unextwebservices.dto.validation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidationUpdateDTO {

    @NotNull(message = "El estado es obligatorio")
    private String status;
    private String observations;
}