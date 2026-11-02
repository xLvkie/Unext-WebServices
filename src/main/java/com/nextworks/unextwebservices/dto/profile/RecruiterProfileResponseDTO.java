package com.nextworks.unextwebservices.dto.profile;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class RecruiterProfileResponseDTO {
    private UUID id;
    private UUID userId;
    private String companyName;
    private String ruc;
    private String industry;
    private String headline;
    private String websiteUrl;
    private String description;
    private String companyLogoUrl;
    private String bannerUrl;
    private String bannerText;
    private Boolean isValidated;
    private Boolean isVerified;
    private Integer validationsCount;
    private String pendingVerificationCode;
}