package com.nextworks.unextwebservices.dto.profile;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class InstitutionProfileResponseDTO {
    private UUID id;
    private UUID userId;
    private String name;
    private String domain;
    private String logoUrl;
    private String bannerUrl;
    private String bannerText;
    private String description;
    private Boolean isVerified;
    private String pendingVerificationCode;
}
