package com.nextworks.unextwebservices.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class RecruiterProfileRequestDTO {
    @NotBlank(message = "El nombre de la compañía es obligatorio")
    private String companyName;

    @NotBlank(message = "El RUC es obligatorio")
    private String ruc;

    private String industry;
    private String description;
}
