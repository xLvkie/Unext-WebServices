package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.agent.tools.AgentRuleGuard;
import com.nextworks.unextwebservices.dto.agent.BioOptimizeRequestDTO;
import com.nextworks.unextwebservices.dto.agent.BioOptimizeResponseDTO;
import com.nextworks.unextwebservices.dto.agent.ChatRequestDTO;
import com.nextworks.unextwebservices.dto.agent.ChatResponseDTO;
import com.nextworks.unextwebservices.dto.agent.EmployabilityInsightsRequestDTO;
import com.nextworks.unextwebservices.dto.agent.EmployabilityInsightsResponseDTO;
import com.nextworks.unextwebservices.dto.agent.JobBlueprintSuggestionDTO;
import com.nextworks.unextwebservices.dto.agent.JobDesignRequestDTO;
import com.nextworks.unextwebservices.dto.agent.JobSuggestionListDTO;
import com.nextworks.unextwebservices.dto.agent.TalentAnalysisRequestDTO;
import com.nextworks.unextwebservices.dto.agent.TalentAnalysisResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "13. Agente IA UNEXT", description = "Agente conversacional con memoria en PostgreSQL, RAG, Tools y casos de uso por segmento")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentRuleGuard agentRuleGuard;

    @Operation(summary = "Chat conversacional con memoria persistente, RAG y tools aisladas por rol")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/chat")
    public ChatResponseDTO chat(
            Authentication authentication,
            @Valid @RequestBody ChatRequestDTO req
    ) {
        User user = agentRuleGuard.requireRole(authentication, Role.POSTULANT, Role.RECRUITER, Role.INSTITUTION);
        return new ChatResponseDTO(agentService.chat(user, req.message()));
    }

    @Operation(summary = "Sugerencias estructuradas en JSON para vacantes o perfiles")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/suggest")
    public JobSuggestionListDTO suggest(
            Authentication authentication,
            @Valid @RequestBody ChatRequestDTO req
    ) {
        User user = agentRuleGuard.requireRole(authentication, Role.POSTULANT, Role.RECRUITER, Role.INSTITUTION);
        return agentService.suggest(user, req.message());
    }

    @Operation(summary = "Optimizador transversal de biografía o descripción ('Acerca de') según segmento")
    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/optimize-bio")
    public BioOptimizeResponseDTO optimizeBio(
            Authentication authentication,
            @Valid @RequestBody BioOptimizeRequestDTO req
    ) {
        User user = agentRuleGuard.requireRole(authentication, Role.POSTULANT, Role.RECRUITER, Role.INSTITUTION);
        return agentService.optimizeBio(user, req.bio());
    }

    @Operation(summary = "Análisis de brecha de habilidades para postulantes (StudentSkill vs JobOffer)")
    @PreAuthorize("hasAuthority('POSTULANT')")
    @PostMapping("/postulant/talent-analysis")
    public TalentAnalysisResponseDTO analyzePostulantTalent(
            Authentication authentication,
            @Valid @RequestBody(required = false) TalentAnalysisRequestDTO req
    ) {
        User user = agentRuleGuard.requireRole(authentication, Role.POSTULANT);
        String targetRole = req != null ? req.targetRole() : null;
        return agentService.analyzePostulantTalent(user, targetRole);
    }

    @Operation(summary = "Diseñador y optimizador de vacantes IA para reclutadores (salarios en S/.)")
    @PreAuthorize("hasAuthority('RECRUITER')")
    @PostMapping("/recruiter/design-job")
    public JobBlueprintSuggestionDTO designJob(
            Authentication authentication,
            @Valid @RequestBody JobDesignRequestDTO req
    ) {
        User user = agentRuleGuard.requireRole(authentication, Role.RECRUITER);
        return agentService.designJob(user, req.prompt());
    }

    @Operation(summary = "Diagnóstico ejecutivo de empleabilidad IA para instituciones educativas")
    @PreAuthorize("hasAuthority('INSTITUTION')")
    @PostMapping("/institution/employability-insights")
    public EmployabilityInsightsResponseDTO generateEmployabilityInsights(
            Authentication authentication,
            @Valid @RequestBody(required = false) EmployabilityInsightsRequestDTO req
    ) {
        User user = agentRuleGuard.requireRole(authentication, Role.INSTITUTION);
        String career = req != null ? req.career() : null;
        String focusQuestion = req != null ? req.focusQuestion() : null;
        return agentService.generateEmployabilityInsights(user, career, focusQuestion);
    }
}
