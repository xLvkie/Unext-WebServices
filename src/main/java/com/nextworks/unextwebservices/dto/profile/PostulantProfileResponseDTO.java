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
    private String institutionalEmail;
    private String academicStatus;
    private String academicEvidenceUrl;
    private String academicEvidenceName;
    private String avatarUrl;
    private String bannerUrl;
    private String bannerText;
    private String cvUrl;
    private String cvFileName;
    private String cvFileSize;
    private String headline;
    private String bio;
    private Boolean hasUniversityBase;
    private Boolean isInstitutionVerified;
    private UUID institutionProfile;
    private String institutionName;
    private String institutionLogoUrl;
    private List<StudentSkillResponseDTO> skills;
}