package com.nextworks.unextwebservices.dto.profile;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

// LO QUE SE VE POR PANTALLA (FRONTEND)

@Data
@Builder
public class RecruiterProfileResponseDTO {
    private UUID id;
    private UUID userId;
    private String companyName;
    private String ruc;
    private String industry;
    private String description;
    private Boolean isValidated;
}
