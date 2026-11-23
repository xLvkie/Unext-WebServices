package com.nextworks.unextwebservices.dto.agent;

import java.util.List;

public record BioOptimizeResponseDTO(
        String role,
        String originalBio,
        String optimizedBio,
        List<String> keyImprovements
) {}
