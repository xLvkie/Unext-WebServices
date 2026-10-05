package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.AgreementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgreementUpdateRequestDTO {
    @NotNull(message = "El nuevo estado es obligatorio")
    private AgreementStatus status;
    private String observations;
}
