package com.nextworks.unextwebservices.dto.review;

import com.nextworks.unextwebservices.entity.enums.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponseDTO {
    private UUID id;
    private UUID agreementId;
    private String companyName;
    private String postulantName;
    private Integer stars;
    private String comment;
    private ReviewStatus status;
    private LocalDateTime createdAt;
}
