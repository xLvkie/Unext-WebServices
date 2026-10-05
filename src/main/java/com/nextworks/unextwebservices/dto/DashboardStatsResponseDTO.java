package com.nextworks.unextwebservices.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponseDTO {
    private long totalVerifiedStudents;
    private long totalHiredStudents;
    private long totalActiveAgreements;
    private long totalEndorsedCompanies;
    private double employabilityRate;
}
