package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponseDTO {
    private UUID id;
    private String jobTitle;
    private String companyName;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
}