package com.nextworks.unextwebservices.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterProfileUpdateDTO {
    @Size(max = 120)
    private String companyName;

    @Size(max = 100)
    private String industry;

    @Size(max = 120)
    private String headline;

    // private String ruc;

    private String websiteUrl;

    @Size(max = 1500)
    private String description;

    @Size(max = 35)
    private String bannerText;

    private String companyLogoUrl;
    private String bannerUrl;
}