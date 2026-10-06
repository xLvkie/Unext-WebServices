package com.nextworks.unextwebservices.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryResponseDTO {
    private String career;
    private long totalVerifiedStudents;
    private long totalHiredStudents;
    private long totalActiveAgreements;
    private long totalEndorsedCompanies;
    private double employabilityRate;
    private String message;
}
