package com.nextworks.unextwebservices.controller;

import com.nextworks.unextwebservices.dto.agent.ChatRequestDTO;
import com.nextworks.unextwebservices.dto.agent.ChatResponseDTO;
import com.nextworks.unextwebservices.dto.agent.JobSuggestionListDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.AgentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "13. Agente IA UNEXT", description = "Agente conversacional con memoria en PostgreSQL y respuestas estructuradas")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/chat")
    public ChatResponseDTO chat(
            Authentication authentication,
            @Valid @RequestBody ChatRequestDTO req
    ) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return new ChatResponseDTO(agentService.chat(user, req.message()));
    }

    @PreAuthorize("hasAnyAuthority('POSTULANT', 'RECRUITER', 'INSTITUTION')")
    @PostMapping("/suggest")
    public JobSuggestionListDTO suggest(
            Authentication authentication,
            @Valid @RequestBody ChatRequestDTO req
    ) {
        User user = (User) authentication.getPrincipal();
        assert user != null;
        return agentService.suggest(user, req.message());
    }
}