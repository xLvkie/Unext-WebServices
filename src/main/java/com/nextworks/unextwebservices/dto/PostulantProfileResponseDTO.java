package com.nextworks.unextwebservices.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PostulantProfileResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String studentCode;
    private String career;
    private Integer currentCycle;
    private String cvUrl;
    private String headline;
    private String bio;
    private Boolean hasUniversityBase;
}
