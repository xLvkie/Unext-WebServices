package com.nextworks.unextwebservices.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DemandByCareerResponseDTO {
    private List<CareerDemandDTO> careers;
    private String message;
}
