package com.nextworks.unextwebservices.dto;
import lombok.Data;

// LO QUE SE PUEDE EDITAR EN EL PERFIL

@Data
public class InstitutionProfileUpdate {
    private String name;
    private String domain;
    private String logoUrl;
    private String description;
}
