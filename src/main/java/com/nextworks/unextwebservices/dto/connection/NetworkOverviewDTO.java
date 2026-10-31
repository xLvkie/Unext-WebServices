package com.nextworks.unextwebservices.dto.connection;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NetworkOverviewDTO {
    private long connectionsCount;
    private long receivedInvitationsCount;
    private long sentInvitationsCount;
    private long followingCount;
    private List<NetworkPersonCardDTO> receivedInvitations;
    private List<NetworkPersonCardDTO> sentInvitations;
    private List<NetworkPersonCardDTO> connections;
    private List<NetworkPersonCardDTO> following;
    private List<NetworkPersonCardDTO> recommendations;
}