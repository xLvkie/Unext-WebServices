package com.nextworks.unextwebservices.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterProfileRequestDTO {
    @NotBlank(message = "El nombre de la compañía es obligatorio")
    @Size(max = 120, message = "El nombre de la empresa no puede superar los 120 caracteres")
    private String companyName;

    @NotBlank(message = "El RUC es obligatorio")
    private String ruc;

    @Size(max = 100)
    private String industry;

    @Size(max = 120, message = "El titular corporativo no puede superar los 120 caracteres")
    private String headline;

    private String websiteUrl;

    @Size(max = 1500, message = "La descripción no puede superar los 1500 caracteres")
    private String description;

    @Size(max = 35, message = "El texto del banner no puede superar los 35 caracteres")
    private String bannerText;

    private String companyLogoUrl;
    private String bannerUrl;
}