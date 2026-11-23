package com.nextworks.unextwebservices.dto.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BioOptimizeRequestDTO(
        @JsonAlias({"draftBio", "description", "content", "message"})
        @NotBlank(message = "El borrador de la biografía o descripción no puede estar vacío")
        @Size(max = 2000, message = "El texto no puede superar los 2000 caracteres")
        String bio
) {}
