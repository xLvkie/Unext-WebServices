package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.AgreementStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AgreementResponseDTO {
    private UUID id;
    private String title;
    private String companyName;
    private String studentName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer weeklyHours;
    private String documentUrl;
    private AgreementStatus status;
    private String observations;
    private LocalDateTime createdAt;
}
