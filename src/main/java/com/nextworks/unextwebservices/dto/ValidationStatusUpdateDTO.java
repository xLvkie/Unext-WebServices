package com.nextworks.unextwebservices.dto;

import com.nextworks.unextwebservices.entity.ValidationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationStatusUpdateDTO {

    @NotNull(message = "El nuevo estado no puede estar vacío (APPROVED, REJECTED)")
    private ValidationStatus status;
}
