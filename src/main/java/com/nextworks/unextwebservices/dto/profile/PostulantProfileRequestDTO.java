package com.nextworks.unextwebservices.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostulantProfileRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    private String studentCode;
    private String career;
    private Integer currentCycle;
}