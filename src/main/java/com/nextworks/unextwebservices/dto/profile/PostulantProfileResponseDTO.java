package com.nextworks.unextwebservices.dto.profile;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.util.List;

@Data
@Builder
public class PostulantProfileResponseDTO {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String studentCode;
    private String career;
    private Integer currentCycle;
    private String cvUrl;
    private String headline;
    private String bio;
    private Boolean hasUniversityBase;
    private Boolean isInstitutionVerified;
    private UUID institutionProfile;
    private List<StudentSkillResponseDTO> skills;
}
