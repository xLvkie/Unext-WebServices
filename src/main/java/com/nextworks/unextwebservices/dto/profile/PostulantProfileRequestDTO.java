package com.nextworks.unextwebservices.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostulantProfileRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 60, message = "El apellido no puede superar los 60 caracteres")
    private String lastName;

    @Size(max = 120, message = "El titular no puede superar los 120 caracteres")
    private String headline;

    @Size(max = 1000, message = "El resumen no puede superar los 1000 caracteres")
    private String bio;

    @Size(max = 35, message = "El título del banner no puede superar los 35 caracteres")
    private String bannerText;

    private String avatarUrl;
    private String bannerUrl;
    private String cvUrl;
    private String cvFileName;
    private String cvFileSize;

    // por definir
    private String studentCode;
    private String career;
    private Integer currentCycle;
}