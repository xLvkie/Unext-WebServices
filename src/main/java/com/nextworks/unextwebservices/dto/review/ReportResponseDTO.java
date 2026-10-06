package com.nextworks.unextwebservices.dto.review;

import com.nextworks.unextwebservices.entity.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ReportResponseDTO {
    private UUID id;
    private UUID reviewId;
    private ReportStatus status;
    private String message;
}
