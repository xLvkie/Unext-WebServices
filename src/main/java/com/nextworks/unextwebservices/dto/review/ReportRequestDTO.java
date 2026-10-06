package com.nextworks.unextwebservices.dto.review;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportRequestDTO {
    @NotBlank(message = "El motivo del reporte es obligatorio")
    private String reason;
}
