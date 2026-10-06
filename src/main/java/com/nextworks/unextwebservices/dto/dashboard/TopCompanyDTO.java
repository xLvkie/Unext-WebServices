package com.nextworks.unextwebservices.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TopCompanyDTO {
    private UUID recruiterId;
    private String companyName;
    private Long hiredStudents;
    private Boolean isValidated;
}
