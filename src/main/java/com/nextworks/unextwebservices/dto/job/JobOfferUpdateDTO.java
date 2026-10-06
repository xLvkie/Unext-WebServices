package com.nextworks.unextwebservices.dto.job;
import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobOfferUpdateDTO {
    private String companyName;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private JobModality modality;
    private ExperienceLevel experienceLevel;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
}
