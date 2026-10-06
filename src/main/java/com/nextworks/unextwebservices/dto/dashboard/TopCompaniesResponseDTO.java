package com.nextworks.unextwebservices.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TopCompaniesResponseDTO {
    private String career;
    private List<TopCompanyDTO> companies;
    private String message;
}
