package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.agent.rag.UnextRagAdvisor;
import com.nextworks.unextwebservices.agent.tools.AgentRuleGuard;
import com.nextworks.unextwebservices.agent.tools.InstitutionTools;
import com.nextworks.unextwebservices.agent.tools.PostulantTools;
import com.nextworks.unextwebservices.agent.tools.RecruiterTools;
import com.nextworks.unextwebservices.dto.agent.BioOptimizeResponseDTO;
import com.nextworks.unextwebservices.dto.agent.EmployabilityInsightsResponseDTO;
import com.nextworks.unextwebservices.dto.agent.JobBlueprintSuggestionDTO;
import com.nextworks.unextwebservices.dto.agent.JobSuggestionListDTO;
import com.nextworks.unextwebservices.dto.agent.TalentAnalysisResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.Role;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Service
public class AgentService {

    private static final String FALLBACK_MODEL = "gemini-3.5-flash-lite";

    private final ChatClient chatClient;
    private final ChatClient statelessChatClient;
    private final ProfileService profileService;
    private final RagContextService ragContextService;
    private final AgentRuleGuard agentRuleGuard;
    private final PostulantTools postulantTools;
    private final RecruiterTools recruiterTools;
    private final InstitutionTools institutionTools;

    public AgentService(
            ChatClient chatClient,
            @Qualifier("statelessChatClient") ChatClient statelessChatClient,
            ProfileService profileService,
            RagContextService ragContextService,
            AgentRuleGuard agentRuleGuard,
            PostulantTools postulantTools,
            RecruiterTools recruiterTools,
            InstitutionTools institutionTools
    ) {
        this.chatClient = chatClient;
        this.statelessChatClient = statelessChatClient;
        this.profileService = profileService;
        this.ragContextService = ragContextService;
        this.agentRuleGuard = agentRuleGuard;
        this.postulantTools = postulantTools;
        this.recruiterTools = recruiterTools;
        this.institutionTools = institutionTools;
    }

    /**
     * Chat conversacional con memoria aislada por usuario (JWT), RAG desde PostgreSQL
     * y tools restringidas estrictamente al segmento del usuario autenticado.
     */
    public String chat(User user, String message) {
        return with503Fallback(model -> executeChat(user, message, model));
    }

    private String executeChat(User user, String message, String fallbackModel) {
        String displayName = profileService.resolveDisplayName(user);
        Object roleTools = toolForRole(user.getRole());

        return agentRuleGuard.executeWithUser(user, () -> {
            var promptSpec = chatClient.prompt()
                    .system(s -> s.param("today", LocalDate.now())
                            .param("name", displayName)
                            .param("role", user.getRole().name()))
                    .user(message)
                    .tools(roleTools)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user.getId().toString())
                            .param(UnextRagAdvisor.USER_CONTEXT_PARAM, user));

            if (fallbackModel != null) {
                promptSpec.options(ChatOptions.builder().model(fallbackModel).temperature(0.3));
            }

            return promptSpec.call().content();
        });
    }

    /**
     * Respuesta estructurada (.entity) con temperatura 0.1 para sugerir borradores
     * de vacantes o perfiles técnicos en formato JSON tipado.
     */
    public JobSuggestionListDTO suggest(User user, String message) {
        String displayName = profileService.resolveDisplayName(user);
        return agentRuleGuard.executeWithUser(user, () ->
                with503Fallback(fallbackModel -> {
                    var promptSpec = chatClient.prompt()
                            .options(buildOptions(0.1, fallbackModel))
                            .system(s -> s.param("today", LocalDate.now())
                                    .param("name", displayName)
                                    .param("role", user.getRole().name()))
                            .user(message)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user.getId().toString())
                                    .param(UnextRagAdvisor.USER_CONTEXT_PARAM, user));
                    return promptSpec.call().entity(JobSuggestionListDTO.class);
                })
        );
    }

    /**
     * Caso de uso transversal (POSTULANT, RECRUITER, INSTITUTION):
     * Optimiza la biografía o descripción corporativa/institucional manteniendo veracidad.
     */
    public BioOptimizeResponseDTO optimizeBio(User user, String rawBio) {
        String originalBio = rawBio.trim();
        String roleGuidance = switch (user.getRole()) {
            case POSTULANT -> "Enfoque en empleabilidad universitaria peruana, logros reales, stack técnico e interés en prácticas o primer empleo.";
            case RECRUITER -> "Enfoque en marca empleadora, propuesta de valor para talento universitario y claridad corporativa.";
            case INSTITUTION -> "Enfoque en prestigio académico, formación de talento, acreditación de competencias y convenios de prácticas.";
        };

        String prompt = """
                Optimiza y formaliza el siguiente borrador de sección "Acerca de" / biografía para el segmento %s en UNEXT.
                %s
                Reglas obligatorias:
                1. Responde siempre en español claro y profesional.
                2. Mantén el 100%% de la veracidad del texto original: NO inventes títulos, certificaciones, empresas, años de experiencia ni métricas inexistentes.
                3. Devuelve `optimizedBio` listo para publicar y entre 2 y 4 `keyImprovements` concretas aplicadas.

                Borrador original:
                \"\"\"
                %s
                \"\"\"
                """.formatted(user.getRole().name(), roleGuidance, originalBio);

        BioDraftOutput output = callStatelessEntity(user, prompt, 0.3, BioDraftOutput.class);
        return new BioOptimizeResponseDTO(
                user.getRole().name(),
                originalBio,
                output != null && output.optimizedBio() != null ? output.optimizedBio().trim() : originalBio,
                output != null && output.keyImprovements() != null ? output.keyImprovements() : List.of()
        );
    }

    /**
     * Caso de uso POSTULANT:
     * Analiza brecha de habilidades reales (StudentSkill) frente a vacantes activas (JobOffer) en unext_db.
     */
    public TalentAnalysisResponseDTO analyzePostulantTalent(User user, String targetRole) {
        RagContextService.TalentSnapshot snapshot = ragContextService.buildTalentSnapshot(user, targetRole);

        String prompt = """
                Realiza un análisis breve y accionable de brecha de habilidades en español para el postulante %s (%s).
                Objetivo profesional indicado: %s
                Habilidades actuales registradas en unext_db: %s
                Habilidades coincidentes con vacantes activas: %s
                Habilidades clave demandadas que aún no registra: %s
                Total de vacantes activas analizadas: %d
                Detalle de vacantes activas relevantes:
                %s

                Instrucciones:
                - Redacta `marketCompatibilitySummary` (3 a 4 oraciones claras, sin inventar vacantes ni habilidades fuera de los datos provistos).
                - Genera `actionPlan` con 3 a 5 pasos concretos de mejora técnica o de portafolio para aumentar su empleabilidad en Perú.
                """.formatted(
                snapshot.postulantName(),
                snapshot.career(),
                (targetRole != null && !targetRole.isBlank()) ? targetRole.trim() : "Empleabilidad general según su carrera",
                snapshot.currentSkills().isEmpty() ? "Ninguna registrada aún" : String.join(", ", snapshot.currentSkills()),
                snapshot.matchedDemandSkills().isEmpty() ? "Ninguna coincidencia aún" : String.join(", ", snapshot.matchedDemandSkills()),
                snapshot.missingKeySkills().isEmpty() ? "Ninguna brecha detectada" : String.join(", ", snapshot.missingKeySkills()),
                snapshot.activeVacanciesAnalyzed(),
                snapshot.marketDetail().isBlank() ? "Sin vacantes activas registradas." : snapshot.marketDetail()
        );

        TalentNarrativeOutput narrative = callStatelessEntity(user, prompt, 0.2, TalentNarrativeOutput.class);

        return new TalentAnalysisResponseDTO(
                snapshot.postulantName(),
                snapshot.career(),
                snapshot.currentSkills(),
                snapshot.matchedDemandSkills(),
                snapshot.missingKeySkills(),
                snapshot.activeVacanciesAnalyzed(),
                narrative != null && narrative.marketCompatibilitySummary() != null
                        ? narrative.marketCompatibilitySummary().trim()
                        : "Análisis generado con base en tu perfil y las vacantes activas en UNEXT.",
                narrative != null && narrative.actionPlan() != null
                        ? narrative.actionPlan()
                        : List.of()
        );
    }

    /**
     * Caso de uso RECRUITER:
     * Diseña una vacante estructurada en JSON tipado (JobBlueprintSuggestionDTO) con salarios en Soles (S/.).
     */
    public JobBlueprintSuggestionDTO designJob(User user, String jobIdea) {
        String prompt = """
                Diseña y optimiza una propuesta completa de vacante en español para el mercado laboral universitario del Perú.
                Idea o puesto solicitado por el reclutador: "%s"

                Reglas obligatorias para los campos JSON:
                - `roleTitle`: título profesional claro y optimizado en español.
                - `suggestedModality`: uno de `REMOTE`, `HYBRID` o `ONSITE`.
                - `suggestedExperienceLevel`: uno de `TRAINEE`, `JUNIOR`, `SEMI_SENIOR` o `SENIOR`.
                - `estimatedSalaryRangeSoles`: rango salarial realista en Perú expresado en soles (ej. "S/. 1,200 - S/. 1,800").
                - `recommendedSkills`: lista de entre 4 y 6 habilidades técnicas clave concretas.
                - `optimizedDescription`: descripción atractiva con responsabilidades y requisitos claros.
                """.formatted(jobIdea.trim());

        return callStatelessEntity(user, prompt, 0.2, JobBlueprintSuggestionDTO.class);
    }

    /**
     * Caso de uso INSTITUTION:
     * Genera diagnóstico ejecutivo en español basado en métricas reales de empleabilidad y convenios.
     */
    public EmployabilityInsightsResponseDTO generateEmployabilityInsights(User user, String career, String focusQuestion) {
        RagContextService.InstitutionSnapshot snapshot = ragContextService.buildInstitutionSnapshot(user, career);

        String prompt = """
                Genera un diagnóstico ejecutivo de empleabilidad en español para la institución "%s".
                Filtro de carrera aplicado: %s
                Métricas reales en unext_db:
                - Alumnos verificados: %d
                - Alumnos contratados (ACCEPTED): %d
                - Tasa de empleabilidad: %.2f%%
                - Convenios de prácticas aprobados: %d
                - Empresas aliadas acreditadas: %d
                - Demanda por carrera: %s
                - Top empresas contratantes: %s
                - Nota de datos: %s
                Enfoque adicional solicitado: %s

                Instrucciones:
                - Usa exclusivamente las cifras reales provistas arriba (no inventes porcentajes ni empresas).
                - Redacta `executiveSummary` (resumen ejecutivo claro y directo).
                - Incluye 3 a 4 `keyFindings` y 3 a 4 `strategicRecommendations` prácticas para mejorar la inserción laboral y convenios.
                """.formatted(
                snapshot.institutionName(),
                snapshot.careerFilter() != null ? snapshot.careerFilter() : "Todas las carreras",
                snapshot.totalVerifiedStudents(),
                snapshot.totalHiredStudents(),
                snapshot.employabilityRate(),
                snapshot.totalActiveAgreements(),
                snapshot.totalEndorsedCompanies(),
                snapshot.careerSummary(),
                snapshot.companiesSummary(),
                snapshot.noDataMessage() != null ? snapshot.noDataMessage() : "Datos suficientes disponibles",
                (focusQuestion != null && !focusQuestion.isBlank()) ? focusQuestion.trim() : "Diagnóstico integral"
        );

        InsightsNarrativeOutput narrative = callStatelessEntity(user, prompt, 0.2, InsightsNarrativeOutput.class);

        return new EmployabilityInsightsResponseDTO(
                snapshot.institutionName(),
                snapshot.careerFilter(),
                snapshot.totalVerifiedStudents(),
                snapshot.totalHiredStudents(),
                snapshot.totalActiveAgreements(),
                snapshot.totalEndorsedCompanies(),
                snapshot.employabilityRate(),
                narrative != null && narrative.executiveSummary() != null
                        ? narrative.executiveSummary().trim()
                        : "Diagnóstico generado a partir de las métricas actuales de la institución.",
                narrative != null && narrative.keyFindings() != null ? narrative.keyFindings() : List.of(),
                narrative != null && narrative.strategicRecommendations() != null ? narrative.strategicRecommendations() : List.of()
        );
    }

    private <T> T callStatelessEntity(User user, String prompt, double temperature, Class<T> responseType) {
        String displayName = profileService.resolveDisplayName(user);
        return agentRuleGuard.executeWithUser(user, () ->
                with503Fallback(fallbackModel -> statelessChatClient.prompt()
                        .options(buildOptions(temperature, fallbackModel))
                        .system(s -> s.param("today", LocalDate.now())
                                .param("name", displayName)
                                .param("role", user.getRole().name()))
                        .user(prompt)
                        .advisors(a -> a.param(UnextRagAdvisor.USER_CONTEXT_PARAM, user))
                        .call()
                        .entity(responseType))
        );
    }

    private ChatOptions.Builder<?> buildOptions(double temperature, String fallbackModel) {
        var builder = ChatOptions.builder().temperature(temperature);
        if (fallbackModel != null) {
            builder.model(fallbackModel);
        }
        return builder;
    }

    private <T> T with503Fallback(Function<String, T> call) {
        try {
            return call.apply(null);
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("503")) {
                return call.apply(FALLBACK_MODEL);
            }
            throw ex;
        }
    }

    private Object toolForRole(Role role) {
        return switch (role) {
            case POSTULANT -> postulantTools;
            case RECRUITER -> recruiterTools;
            case INSTITUTION -> institutionTools;
        };
    }

    private record BioDraftOutput(String optimizedBio, List<String> keyImprovements) {}

    private record TalentNarrativeOutput(String marketCompatibilitySummary, List<String> actionPlan) {}

    private record InsightsNarrativeOutput(
            String executiveSummary,
            List<String> keyFindings,
            List<String> strategicRecommendations
    ) {}
}
