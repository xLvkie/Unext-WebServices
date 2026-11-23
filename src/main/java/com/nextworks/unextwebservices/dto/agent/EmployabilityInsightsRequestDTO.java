package com.nextworks.unextwebservices.dto.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

public record EmployabilityInsightsRequestDTO(
        @Size(max = 150, message = "El filtro de carrera no puede superar los 150 caracteres")
        String career,
        @JsonAlias({"question", "message"})
        @Size(max = 1000, message = "El enfoque adicional no puede superar los 1000 caracteres")
        String focusQuestion
) {}
