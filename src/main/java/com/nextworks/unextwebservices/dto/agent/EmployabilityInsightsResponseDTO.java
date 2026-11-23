package com.nextworks.unextwebservices.dto.agent;

import java.util.List;

public record EmployabilityInsightsResponseDTO(
        String institutionName,
        String careerFilter,
        long totalVerifiedStudents,
        long totalHiredStudents,
        long totalActiveAgreements,
        long totalEndorsedCompanies,
        double employabilityRate,
        String executiveSummary,
        List<String> keyFindings,
        List<String> strategicRecommendations
) {}
