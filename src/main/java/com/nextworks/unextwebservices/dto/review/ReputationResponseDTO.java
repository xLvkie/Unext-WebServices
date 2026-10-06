package com.nextworks.unextwebservices.dto.review;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReputationResponseDTO {
    private Double averageStars;
    private Integer totalReviews;
    private List<ReviewResponseDTO> reviews;
}
