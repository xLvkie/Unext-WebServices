package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.job.ApplicationResponseDTO;
import com.nextworks.unextwebservices.dto.job.JobOfferResponseDTO;
import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'INSTITUTION', 'RECRUITER')")
    @GetMapping
    public ResponseEntity<List<JobOfferResponseDTO>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobModality modality,
            @RequestParam(required = false) ExperienceLevel experience) {

        return ResponseEntity.ok(jobService.searchJobs(keyword, modality, experience));
        /*
        Retorna un listado de todos los trabajos
        El user requiere uso del token
         */
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @PostMapping("/{jobId}/apply")
    public ResponseEntity<ApplicationResponseDTO> applyToJob(
            Authentication authentication,
            @PathVariable UUID jobId) {

        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(jobService.applyToJob(user.getEmail(), jobId));
        /*
        Envia su solicitud de aplicación a la vacante mediante el <ID DE LA VACANTE>
        El user requiere uso del token
         */
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @GetMapping("/applications/me")
    public ResponseEntity<List<ApplicationResponseDTO>> getMyApplications(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(jobService.getMyApplications(user.getEmail()));
        /*
        Retorna un listado de todas las postulaciones de vacantes
        El user requiere uso del token
         */
    }
}