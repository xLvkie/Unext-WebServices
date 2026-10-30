package com.nextworks.unextwebservices.dto.connection;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ConnectionStatusResponseDTO {
    private UUID connectionId;
    private UUID targetUserId;
    private String status;
    private boolean canConnect;
    private String restrictionReason;
}