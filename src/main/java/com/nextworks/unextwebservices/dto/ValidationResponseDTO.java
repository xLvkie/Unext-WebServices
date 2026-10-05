package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.ValidationStatus;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ValidationResponseDTO {
    private UUID id;
    private String institutionName;
    private String technicalSkillName;
    private String evidenceUrl;
    private ValidationStatus status;
}
