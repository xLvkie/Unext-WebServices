package com.nextworks.unextwebservices.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class RecruiterDirectoryResponseDTO {
    private UUID id;
    private String companyName;
}
