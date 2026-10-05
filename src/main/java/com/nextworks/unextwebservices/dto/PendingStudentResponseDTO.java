package com.nextworks.unextwebservices.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PendingStudentResponseDTO {
    private UUID postulantId;
    private String firstName;
    private String lastName;
    private String email;
    // Variable visible en el frontend
    private Boolean isVerified;
}
