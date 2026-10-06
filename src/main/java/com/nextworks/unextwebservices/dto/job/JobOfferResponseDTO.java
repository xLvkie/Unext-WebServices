package com.nextworks.unextwebservices.dto.job;

import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JobOfferResponseDTO {
    private UUID id;
    private String companyName;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private JobModality modality;
    private ExperienceLevel experienceLevel;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private LocalDateTime createdAt;
}
