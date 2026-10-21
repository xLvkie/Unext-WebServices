package com.nextworks.unextwebservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UnextWebServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnextWebServicesApplication.class, args);
    }

}
