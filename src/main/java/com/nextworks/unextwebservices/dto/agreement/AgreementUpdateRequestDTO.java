package com.nextworks.unextwebservices.dto.agreement;

import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgreementUpdateRequestDTO {
    @NotNull(message = "El nuevo estado es obligatorio")
    private AgreementStatus status;
    private String observations;
}
