package com.nextworks.unextwebservices.dto.agent;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record JobBlueprintSuggestionDTO(
        @NotBlank String roleTitle,
        @NotBlank String suggestedModality,
        @NotBlank String suggestedExperienceLevel,
        String estimatedSalaryRangeSoles,
        List<String> recommendedSkills,
        String optimizedDescription
) {}