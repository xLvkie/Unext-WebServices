package com.nextworks.unextwebservices.dto.job;

import com.nextworks.unextwebservices.entity.enums.ApplicationStatus;
import com.nextworks.unextwebservices.entity.enums.CompatibilityLabel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RecruiterApplicationResponseDTO {
    private UUID id;
    private UUID postulantProfileId;
    private String firstName;
    private String lastName;
    private String career;
    private Integer currentCycle;
    private String headline;
    private Boolean isInstitutionVerified;
    private List<String> skills;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private Integer compatibilityPercent;
    private CompatibilityLabel compatibilityLabel;
    private String matchingHint;
    private Double averageStars;
    private Integer reviewCount;
}
