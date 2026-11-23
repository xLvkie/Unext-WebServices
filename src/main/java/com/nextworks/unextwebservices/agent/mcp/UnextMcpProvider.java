package com.nextworks.unextwebservices.agent.mcp;

import com.nextworks.unextwebservices.agent.tools.AgentRuleGuard;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.RagContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class UnextMcpProvider {

    private final RagContextService ragContextService;
    private final AgentRuleGuard agentRuleGuard;

    @McpResource(
            uri = "unext://resources/active-jobs",
            name = "Vacantes Activas UNEXT",
            description = "Resumen en tiempo real de las vacantes activas registradas en unext_db",
            mimeType = "text/plain"
    )
    public String getActiveJobsResource() {
        requireAuthenticatedUser();
        return ragContextService.getActiveJobsSummary(20);
    }

    @McpResource(
            uri = "unext://resources/my-profile",
            name = "Mi Perfil UNEXT",
            description = "Contexto RAG y datos reales del usuario autenticado según su token JWT",
            mimeType = "text/plain"
    )
    public String getMyProfileResource() {
        User user = requireAuthenticatedUser();
        return ragContextService.retrieveContext(user, "");
    }

    @McpResource(
            uri = "unext://resources/platform-rules",
            name = "Reglas de Negocio UNEXT",
            description = "Reglas oficiales de empleabilidad, convenios TRAINEE y acreditación institucional en UNEXT",
            mimeType = "text/plain"
    )
    public String getPlatformRulesResource() {
        requireAuthenticatedUser();
        return ragContextService.getPlatformRulesContext();
    }

    @McpPrompt(
            name = "unext-optimize-bio",
            description = "Plantilla MCP para optimizar la biografía o descripción según el segmento en UNEXT"
    )
    public String optimizeBioPrompt(
            @McpArg(name = "bio", description = "Borrador de biografía o descripción", required = true)
            String bio
    ) {
        User user = requireAuthenticatedUser();
        return """
                Actúa como el Agente IA oficial de UNEXT (segmento %s).
                Optimiza y formaliza en español el siguiente borrador de sección "Acerca de", manteniendo 100%% la veracidad del texto original:

                %s
                """.formatted(user.getRole().name(), bio != null ? bio.trim() : "");
    }

    @McpPrompt(
            name = "unext-design-job",
            description = "Plantilla MCP para diseñar una vacante optimizada en Soles (S/.)"
    )
    public String designJobPrompt(
            @McpArg(name = "prompt", description = "Puesto o idea de vacante", required = true)
            String prompt
    ) {
        requireAuthenticatedUser();
        return """
                Diseña una propuesta completa de vacante en español para el mercado laboral peruano en UNEXT a partir de esta idea: "%s".
                Incluye título optimizado, modalidad (REMOTE/HYBRID/ONSITE), nivel (TRAINEE/JUNIOR/SEMI_SENIOR/SENIOR),
                rango salarial estimado en Soles (S/.), 4 a 6 habilidades técnicas clave y descripción atractiva.
                """.formatted(prompt != null ? prompt.trim() : "");
    }

    @McpPrompt(
            name = "unext-talent-analysis",
            description = "Plantilla MCP para análisis de brecha de habilidades del postulante"
    )
    public String talentAnalysisPrompt(
            @McpArg(name = "targetRole", description = "Rol objetivo opcional", required = false)
            String targetRole
    ) {
        requireAuthenticatedUser();
        String goal = (targetRole != null && !targetRole.isBlank()) ? targetRole.trim() : "Empleabilidad general";
        return """
                Usa las herramientas obtenerMisHabilidadesPostulante y listarVacantesActivasDisponibles para comparar mis habilidades reales en UNEXT contra la demanda actual enfocada en: %s.
                Detalla habilidades coincidentes, competencias técnicas faltantes y un plan breve de mejora en español.
                """.formatted(goal);
    }

    @McpPrompt(
            name = "unext-employability-insights",
            description = "Plantilla MCP para diagnóstico ejecutivo de empleabilidad institucional"
    )
    public String employabilityInsightsPrompt(
            @McpArg(name = "career", description = "Carrera opcional a filtrar", required = false)
            String career
    ) {
        requireAuthenticatedUser();
        String scope = (career != null && !career.isBlank()) ? "la carrera " + career.trim() : "toda la institución";
        return """
                Usa la herramienta consultarEstadisticasEmpleabilidadInstitucion para elaborar un diagnóstico ejecutivo en español sobre %s.
                Resume hallazgos clave sobre alumnos verificados, tasa de empleabilidad, convenios activos y recomendaciones prácticas.
                """.formatted(scope);
    }

    private User requireAuthenticatedUser() {
        User user = agentRuleGuard.resolveCurrentUserOptional();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión con un token JWT válido");
        }
        return user;
    }
}
