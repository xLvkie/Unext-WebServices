package com.nextworks.unextwebservices.dto.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

public record TalentAnalysisRequestDTO(
        @JsonAlias({"careerGoal", "message", "keyword"})
        @Size(max = 300, message = "El objetivo profesional no puede superar los 300 caracteres")
        String targetRole
) {}
