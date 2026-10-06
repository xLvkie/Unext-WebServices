package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.dashboard.DashboardSummaryResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.DemandByCareerResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.TopCompaniesResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7. Dashboard de Empleabilidad", description = "Métricas y analíticas institucionales sobre demanda laboral, carreras y empresas")
@RestController
@RequestMapping("/api/institution/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponseDTO> getSummary(
            Authentication authentication,
            @RequestParam(required = false) String career) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(dashboardService.getSummary(user.getEmail(), career));
        /*
        Metricas del dashboard con filtro opcional ?career=
        Sin alumnos en esa carrera responde 200 con ceros y message "No hay datos suficientes..."
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/top-companies")
    public ResponseEntity<TopCompaniesResponseDTO> getTopCompanies(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String career) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(dashboardService.getTopCompanies(user.getEmail(), limit, career));
        /*
        Empresas con mayor reclutamiento (alumnos distintos contratados), limit entre 1 y 50
         */
    }

    @PreAuthorize("hasAuthority('INSTITUTION')")
    @GetMapping("/demand-by-career")
    public ResponseEntity<DemandByCareerResponseDTO> getDemandByCareer(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return ResponseEntity.ok(dashboardService.getDemandByCareer(user.getEmail()));
        /*
        Postulaciones y aceptaciones agrupadas por carrera de los alumnos de la institucion
         */
    }
}
