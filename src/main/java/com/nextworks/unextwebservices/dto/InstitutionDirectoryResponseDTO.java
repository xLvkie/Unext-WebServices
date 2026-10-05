package com.nextworks.unextwebservices.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class InstitutionDirectoryResponseDTO {
    private UUID id;
    private String name;
}
