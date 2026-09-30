package com.nextworks.unextwebservices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class RecruiterProfileRequestDTO {
    @NotBlank(message = "El nombre de la compañia es obligatorio")
    private String companyName;

    @NotBlank(message = "El ruc es obligatorio")
    private String ruc;

    private String corporatePosition;
    private String businessSector;
}
