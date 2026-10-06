package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.review.ReportRequestDTO;
import com.nextworks.unextwebservices.dto.review.ReportResponseDTO;
import com.nextworks.unextwebservices.dto.review.ReputationResponseDTO;
import com.nextworks.unextwebservices.dto.review.ReviewRequestDTO;
import com.nextworks.unextwebservices.dto.review.ReviewResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.ReviewService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "8. Reseñas y Reputación", description = "Calificaciones mutuas entre empresas y postulantes, y reportes de comportamiento")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PreAuthorize("hasAuthority('RECRUITER')")
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> saveReview(
            Authentication authentication,
            @Valid @RequestBody ReviewRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(reviewService.saveReview(user.getEmail(), request));
        /*
        Califica un convenio ya finalizado. publish=false guarda borrador; publish=true publica.
         */
    }

    @PreAuthorize("hasAuthority('POSTULANT')")
    @GetMapping("/me")
    public ResponseEntity<ReputationResponseDTO> getMyReputation(
            Authentication authentication,
            @RequestParam(required = false) Integer stars) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(reviewService.getMyReputation(user.getEmail(), stars));
        /*
        Promedio, total y reseñas publicadas. stars filtra el listado por esa calificación.
         */
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER')")
    @PostMapping("/{reviewId}/reports")
    public ResponseEntity<ReportResponseDTO> reportReview(
            Authentication authentication,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReportRequestDTO request) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(reviewService.reportReview(user.getEmail(), reviewId, request));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER')")
    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<String> cancelReport(
            Authentication authentication,
            @PathVariable UUID reportId) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(reviewService.cancelReport(user.getEmail(), reportId));
    }
}
