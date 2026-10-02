package com.nextworks.unextwebservices.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

// LO QUE SE VE POR PANTALLA (FRONTEND)

@Data
@Builder
public class InstitutionProfileResponseDTO {
    private UUID id;
    private String name;
    private String domain;
    private String logoUrl;
    private String description;
}
