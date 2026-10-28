package com.nextworks.unextwebservices.dto.profile;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class StudentSkillResponseDTO {
    private UUID id;
    private String name;
    private String masteryLevel;
    private Boolean isTopSkill;
    private Boolean isValidatedByInstitution;
    private String validatedInstitutionName;
    private String validatedInstitutionLogoUrl;
}