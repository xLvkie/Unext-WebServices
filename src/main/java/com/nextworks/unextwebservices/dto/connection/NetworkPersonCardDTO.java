package com.nextworks.unextwebservices.dto.connection;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NetworkPersonCardDTO {
    private UUID connectionId;
    private UUID userId;
    private UUID profileId;
    private String role;
    private String name;
    private String headline;
    private String subtitle;
    private String avatarUrl;
    private String bannerUrl;
    private String bannerText;
    private boolean isVerified;
    private String connectionStatus;
    private boolean isFollowing;
    private boolean canConnect;
    private String restrictionReason;
    private LocalDateTime createdAt;
}