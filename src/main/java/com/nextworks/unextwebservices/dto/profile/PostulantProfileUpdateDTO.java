package com.nextworks.unextwebservices.dto.profile;

import lombok.Data;

@Data
public class PostulantProfileUpdateDTO {
    private String career;
    private Integer currentCycle;
    private String cvUrl;
    private String headline;
    private String bio;
}
