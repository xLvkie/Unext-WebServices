package com.nextworks.unextwebservices.dto.validation;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PendingValidationResponseDTO {
    private UUID validationId;
    private String studentName;
    private String skillName;
    private String evidenceUrl;
    private String status;
}
