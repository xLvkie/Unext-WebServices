package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponseDTO {
    private String token;
    private UUID userId;
    private Role role;
    private Boolean isProfileCompleted;
}
