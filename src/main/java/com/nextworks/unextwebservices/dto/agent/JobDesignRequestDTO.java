package com.nextworks.unextwebservices.dto.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobDesignRequestDTO(
        @JsonAlias({"roleIdea", "title", "message", "idea"})
        @NotBlank(message = "La idea o puesto de la vacante no puede estar vacío")
        @Size(max = 1000, message = "La idea de la vacante no puede superar los 1000 caracteres")
        String prompt
) {}
