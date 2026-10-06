package com.nextworks.unextwebservices.dto.directory;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class InstitutionDirectoryResponseDTO {
    private UUID id;
    private UUID userId;
    private String name;
}
