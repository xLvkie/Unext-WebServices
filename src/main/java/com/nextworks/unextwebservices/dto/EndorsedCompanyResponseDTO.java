package com.nextworks.unextwebservices.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EndorsedCompanyResponseDTO {
    private UUID recruiterId;
    private String companyName;
    private LocalDateTime endorsedAt;
}
