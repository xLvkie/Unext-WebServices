package com.nextworks.unextwebservices.dto.directory;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PostulantDirectoryResponseDTO {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String career;
    private Boolean isInstitutionVerified; 
}