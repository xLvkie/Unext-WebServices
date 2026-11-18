package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.agent.JobSuggestionListDTO;
import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.RecruiterProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.RecruiterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final ChatClient chatClient;
    private final PostulantProfileRepository postulantRepo;
    private final RecruiterProfileRepository recruiterRepo;
    private final InstitutionProfileRepository institutionRepo;

    /**
     * Sección 4.4: Chat conversacional con memoria aislada por usuario (JWT)
     * y parámetros dinámicos {today}, {name} y {role}.
     */
    public String chat(User user, String message) {
        try {
            return executeChat(user, message, null);
        } catch (Exception ex) {
            // Si gemini-3.8-flash da 503 por alta demanda, reintenta al instante con el modelo lite
            if (ex.getMessage() != null && ex.getMessage().contains("503")) {
                return executeChat(user, message, "gemini-3.8-flash-lite");
            }
            throw ex;
        }
    }

    private String executeChat(User user, String message, String fallbackModel) {
        var promptSpec = chatClient.prompt()
                .system(s -> s.param("today", LocalDate.now())
                        .param("name", nombreDe(user))
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
        return chatClient.prompt()
                .options(ChatOptions.builder()
                        .temperature(0.1))
                .system(s -> s.param("today", LocalDate.now())
                        .param("name", nombreDe(user))
                        .param("role", user.getRole().name()))
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user.getId().toString()))
                .call()
                .entity(JobSuggestionListDTO.class);
    }

    /**
     * Sección 3.4: Busca el nombre real del usuario según su rol en UNEXT
     * (PostulantProfile, RecruiterProfile o InstitutionProfile), cayendo al email si aún no completó perfil.
     */
    private String nombreDe(User user) {
        if (user.getRole() == Role.POSTULANT) {
            return postulantRepo.findByUserId(user.getId())
                    .map(p -> (p.getFirstName() + " " + p.getLastName()).trim())
                    .orElse(user.getEmail());
        }
        if (user.getRole() == Role.RECRUITER) {
            return recruiterRepo.findByUserId(user.getId())
                    .map(RecruiterProfile::getCompanyName)
                    .orElse(user.getEmail());
        }
        if (user.getRole() == Role.INSTITUTION) {
            return institutionRepo.findByUserId(user.getId())
                    .map(InstitutionProfile::getName)
                    .orElse(user.getEmail());
        }
        return user.getEmail();
    }
}