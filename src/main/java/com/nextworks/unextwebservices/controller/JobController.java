package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.ApplicationResponseDTO;
import com.nextworks.unextwebservices.dto.JobOfferResponseDTO;
import com.nextworks.unextwebservices.entity.ExperienceLevel;
import com.nextworks.unextwebservices.entity.JobModality;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<List<JobOfferResponseDTO>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobModality modality,
            @RequestParam(required = false) ExperienceLevel experience) {

        return ResponseEntity.ok(jobService.searchJobs(keyword, modality, experience));
    }

    @PostMapping("/{jobId}/apply")
    public ResponseEntity<ApplicationResponseDTO> applyToJob(
            Authentication authentication,
            @PathVariable UUID jobId) {

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(jobService.applyToJob(user.getEmail(), jobId));
    }

    @GetMapping("/applications/me")
    public ResponseEntity<List<ApplicationResponseDTO>> getMyApplications(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(jobService.getMyApplications(user.getEmail()));
    }
}