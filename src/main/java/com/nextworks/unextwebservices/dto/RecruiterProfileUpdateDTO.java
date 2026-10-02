package com.nextworks.unextwebservices.dto;
import lombok.Data;

// LO QUE SE PUEDE EDITAR EN EL PERFIL

@Data
public class RecruiterProfileUpdateDTO {
    private String companyName;
    private String industry;
    private String description;
}
