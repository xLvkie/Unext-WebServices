package com.nextworks.unextwebservices.dto.job;

import com.nextworks.unextwebservices.entity.enums.CompatibilityLabel;
import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class JobRecommendationResponseDTO {
    private UUID id;
    private String companyName;
    private String title;
    private String description;
    private String location;
    private JobModality modality;
    private ExperienceLevel experienceLevel;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private List<String> requiredSkills;
    private List<String> matchedSkills;
    private Integer compatibilityPercent;
    private CompatibilityLabel compatibilityLabel;
    private LocalDateTime createdAt;
}
