package com.nextworks.unextwebservices.dto.agent;

import java.util.List;

public record TalentAnalysisResponseDTO(
        String postulantName,
        String career,
        List<String> currentSkills,
        List<String> matchedDemandSkills,
        List<String> missingKeySkills,
        int activeVacanciesAnalyzed,
        String marketCompatibilitySummary,
        List<String> actionPlan
) {}
