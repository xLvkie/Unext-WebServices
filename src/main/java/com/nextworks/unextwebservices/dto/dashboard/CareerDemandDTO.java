package com.nextworks.unextwebservices.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CareerDemandDTO {
    private String career;
    private Long applications;
    private Long accepted;
}
