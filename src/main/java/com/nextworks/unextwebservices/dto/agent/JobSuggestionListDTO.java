package com.nextworks.unextwebservices.dto.agent;

import java.util.List;

public record JobSuggestionListDTO(
        List<JobBlueprintSuggestionDTO> suggestions
) {}