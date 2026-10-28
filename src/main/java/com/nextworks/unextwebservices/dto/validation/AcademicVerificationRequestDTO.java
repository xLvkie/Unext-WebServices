package com.nextworks.unextwebservices.dto.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AcademicVerificationRequestDTO {

    @NotNull(message = "Debes seleccionar una institución")
    private UUID institutionProfileId;

    // 'STUDENT' (Caso 2: Estudiante activo) o 'GRADUATE' (Caso 1: Ya terminó / Egresado)
    @NotBlank(message = "El tipo de vínculo académico es obligatorio")
    private String academicStatus;

    // Campos para Caso 2 (Estudiante Activo)
    private String institutionalEmail;
    private String studentCode;
    private String career;
    private Integer currentCycle;

    // Campos para Caso 1 (Egresado / Documento o constancia)
    private String academicEvidenceUrl;
    private String academicEvidenceName;
}
