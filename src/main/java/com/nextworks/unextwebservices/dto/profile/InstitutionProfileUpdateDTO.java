package com.nextworks.unextwebservices.dto.profile;

import lombok.Data;

@Data
public class InstitutionProfileUpdateDTO {
    private String name;
    private String domain;
    private String logoUrl;
    private String bannerUrl;
    private String bannerText;
    private String description;
}