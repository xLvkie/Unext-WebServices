package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.agent.JobSuggestionListDTO;
import com.nextworks.unextwebservices.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AgentService {

    private static final String FALLBACK_MODEL = "gemini-3.5-flash-lite";

    private final ChatClient chatClient;
    private final ProfileService profileService;

    /**
     * Sección 4.4: Chat conversacional con memoria aislada por usuario (JWT)
     * y parámetros dinámicos {today}, {name} y {role}.
     */
    public String chat(User user, String message) {
        try {
            return executeChat(user, message, null);
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("503")) {
                return executeChat(user, message, FALLBACK_MODEL);
            }
            throw ex;
        }
    }

    private String executeChat(User user, String message, String fallbackModel) {
        String displayName = profileService.resolveDisplayName(user);
        var promptSpec = chatClient.prompt()
                .system(s -> s.param("today", LocalDate.now())
                        .param("name", displayName)
                        .param("role", user.getRole().name()))
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user.getId().toString()));

        if (fallbackModel != null) {
            promptSpec.options(ChatOptions.builder().model(fallbackModel).temperature(0.3));
        }

        return promptSpec.call().content();
    }

    /**
     * Sección 5.2: Respuesta estructurada (.entity) con temperatura 0.1
     * para sugerir/diseñar borradores de vacantes o perfiles técnicos en formato JSON tipado.
     */
    public JobSuggestionListDTO suggest(User user, String message) {
        String displayName = profileService.resolveDisplayName(user);
        return chatClient.prompt()
                .options(ChatOptions.builder()
                        .temperature(0.1))
                .system(s -> s.param("today", LocalDate.now())
                        .param("name", displayName)
                        .param("role", user.getRole().name()))
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user.getId().toString()))
                .call()
                .entity(JobSuggestionListDTO.class);
    }
}
