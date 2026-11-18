# AGENTS.md — UNEXT Backend (`unext-web-services`)

Guía técnica y operativa para el agente de desarrollo en **Google Antigravity**. Este documento define el contexto actual del backend de **UNEXT** y la hoja de ruta exacta para implementar **Tools, MCP (Model Context Protocol), RAG (Retrieval-Augmented Generation)** y los casos de uso de IA generativa para los 3 segmentos de la plataforma.

---

## 1. Contexto del Proyecto y Stack Tecnológico

**UNEXT** es una plataforma web interuniversitaria peruana de empleabilidad que conecta a tres segmentos de usuarios:
- `POSTULANT`: Estudiantes universitarios y egresados.
- `RECRUITER`: Empresas y reclutadores.
- `INSTITUTION`: Universidades e institutos acreditadores.

### Stack Actual (No modificar versiones sin aprobación)
- **Lenguaje / Framework:** Java 21, Spring Boot `4.1.1`, Maven.
- **Base de Datos:** PostgreSQL (`unext_db`), Spring Data JPA / Hibernate.
- **Seguridad:** Spring Security Stateless con JWT (`JwtAuthenticationFilter`, `JwtUtil`, `@PreAuthorize`).
- **IA Generativa:** Spring AI `2.0.0` usando `spring-ai-starter-model-openai` apuntando al endpoint compatible de **Google Gemini**:
  - `base-url`: `https://generativelanguage.googleapis.com/v1beta/openai/`
  - `model`: `gemini-3.5-flash-lite`
  - `api-key`: Inyectada desde `.env` mediante `${GEMINI_API_KEY}` (`spring.config.import=optional:file:.env[.properties]`).
- **Memoria de Chat:** `spring-ai-starter-model-chat-memory-repository-jdbc` respaldada por la tabla `SPRING_AI_CHAT_MEMORY` en PostgreSQL (aislada por `user.getId().toString()`).

---

## 2. Estado Actual de la IA (Ya Implementado — Base Existente)

Antes de crear nuevas clases, revisar y respetar los archivos ya funcionales:
- `src/main/resources/prompts/system-prompt.md`: System prompt base con marcadores `{today}`, `{name}`, `{role}`.
- `src/main/java/com/nextworks/unextwebservices/config/AgentConfig.java`: Beans de `ChatMemory` (`maxMessages(20)`) y `ChatClient` con `MessageChatMemoryAdvisor`.
- `src/main/java/com/nextworks/unextwebservices/service/AgentService.java`: Métodos `chat(User, String)` y `suggest(User, String)` con resolución de nombre real por rol (`nombreDe(User)`).
- `src/main/java/com/nextworks/unextwebservices/controller/AgentController.java`: Endpoints `/api/agent/chat` y `/api/agent/suggest`.
- `src/main/java/com/nextworks/unextwebservices/dto/agent/`: `ChatRequestDTO`, `ChatResponseDTO`, `JobBlueprintSuggestionDTO`, `JobSuggestionListDTO`.

---

## 3. Reglas de Arquitectura y Seguridad

1. **Arquitectura en Capas:** Respetar estrictamente `config/`, `controller/`, `dto/`, `entity/`, `repository/`, `security/`, `service/`.
2. **Aislamiento por Rol y Token:**
   - Todos los endpoints nuevos deben estar protegidos con `@PreAuthorize("hasAuthority('...')")` o `@PreAuthorize("hasAnyAuthority(...)")`.
   - El usuario autenticado SIEMPRE se extrae de `Authentication authentication` (`User user = (User) authentication.getPrincipal();`), nunca desde el body del request.
3. **Idioma y Formato:**
   - Todas las respuestas generadas por la IA y mensajes de error deben estar en **español** (contexto laboral peruano, salarios en Soles `S/.`).
   - Usar Java `record` para nuevos DTOs de entrada/salida de IA con validaciones Jakarta (`@NotBlank`, `@Size`).
4. **Protección de Credenciales:** Nunca hardcodear `GEMINI_API_KEY`, contraseñas de BD ni secretos JWT en código fuente. No modificar `.gitignore`.

---

## 4. Casos de Uso de IA a Implementar por Segmento

### A. Transversal (Los 3 segmentos: `POSTULANT`, `RECRUITER`, `INSTITUTION`)
1. **Optimizador de Biografía ("Acerca de"):**
   - Recibe el borrador actual de la biografía del usuario (`bio` o `description`) y su rol.
   - Devuelve una versión formalizada, clara, profesional y orientada a empleabilidad/marca empleadora/prestigio académico según el rol, manteniendo la veracidad del texto original.

### B. Postulante (`POSTULANT`)
2. **Optimización de Talento IA (Brecha de Habilidades):**
   - Consulta las habilidades reales del postulante (`StudentSkillRepository`) y las vacantes activas (`JobOfferRepository`).
   - Analiza qué habilidades de su perfil coinciden con la demanda actual, qué competencias técnicas clave le faltan para mejorar su empleabilidad y sugiere un plan breve de mejora.

### C. Reclutador (`RECRUITER`)
3. **Diseñador y Optimizador de Vacantes IA:**
   - Recibe un puesto o idea breve (ej. *"Practicante de Backend Java"*) y estructura una propuesta completa en JSON tipado (`JobBlueprintSuggestionDTO`): título optimizado, descripción atractiva, requisitos, 4 a 6 habilidades técnicas clave, modalidad sugerida, nivel de experiencia y rango salarial estimado en `S/.`.

### D. Institución (`INSTITUTION`)
4. **Analista de Empleabilidad IA (Diagnóstico Ejecutivo):**
   - Consume las métricas reales de la institución (`InstitutionService.getDashboardStats(...)`, demanda por carrera y convenios activos).
   - Genera un resumen ejecutivo en español con hallazgos clave y recomendaciones prácticas para mejorar la inserción laboral de sus alumnos.

---

## 5. Hoja de Ruta Técnica (Orden de Implementación)

Ejecutar las siguientes fases de forma ordenada, verificando compilación (`./mvnw compile`) tras cada fase:

### Fase 1: Definición de Tools para el Agente (`@Tool` de Spring AI)
Crear un paquete `com.nextworks.unextwebservices.agent.tools` (o `service/tools`) con herramientas invocables por el LLM:
- `PostulantTools`:
  - `@Tool`: Obtener las habilidades registradas y validadas del postulante autenticado.
  - `@Tool`: Listar vacantes activas disponibles (título, modalidad, nivel y habilidades requeridas).
- `RecruiterTools`:
  - `@Tool`: Consultar las vacantes activas publicadas por la empresa del reclutador autenticado.
- `InstitutionTools`:
  - `@Tool`: Consultar estadísticas de empleabilidad, alumnos verificados y convenios activos de la institución autenticada.
- Registrar las tools en `ChatClient` o pasarlas dinámicamente según el rol del usuario en `AgentService` para que un rol nunca invoque herramientas de otro segmento.

### Fase 2: Endpoints de Casos de Uso y Reglas de Servicio
Ampliar `AgentService` y `AgentController` (o crear un controlador dedicado si el laboratorio lo requiere) con:
- `POST /api/agent/optimize-bio` (`POSTULANT`, `RECRUITER`, `INSTITUTION`): Mejora la biografía según el rol.
- `POST /api/agent/postulant/talent-analysis` (`POSTULANT`): Ejecuta el análisis de brecha de habilidades usando los datos reales del perfil y vacantes activas.
- `POST /api/agent/recruiter/design-job` (`RECRUITER`): Devuelve `JobBlueprintSuggestionDTO` estructurado.
- `POST /api/agent/institution/employability-insights` (`INSTITUTION`): Devuelve diagnóstico ejecutivo basado en métricas reales.
- **Controlador de Reglas / Seguridad del Agente:** Centralizar la validación de token, rol y reglas de acceso a herramientas/prompts antes de invocar al modelo.

### Fase 3: RAG (Retrieval-Augmented Generation)
Implementar recuperación de contexto desde la propia base de datos de UNEXT antes de generar respuestas:
1. Crear un servicio `RagContextService` (o `VectorStore` si se habilita `pgvector` en PostgreSQL; si no está disponible `pgvector`, implementar recuperación estructurada vía repositorios JPA + filtrado semántico/por palabras clave sobre `JobOffer`, `StudentSkill` y documentos/reglas de UNEXT).
2. Inyectar el contexto recuperado en el prompt del `ChatClient` (mediante un `Advisor` personalizado o bloque de contexto en el prompt) para que el agente responda siempre fundamentado en datos reales de `unext_db` y sin alucinar vacantes o métricas inexistentes.

### Fase 4: Adaptación a MCP (Model Context Protocol)
Integrar el soporte de **MCP** en Spring AI 2.0:
1. Agregar la dependencia de Spring AI MCP (`spring-ai-starter-mcp-server-webmvc` o el starter MCP correspondiente a Spring AI `2.0.0`).
2. Exponer las herramientas (`Tools`), recursos de datos (resumen de vacantes/perfiles) y prompts del sistema a través del protocolo estándar MCP, manteniendo la validación de seguridad (JWT / Roles) en la capa de transporte HTTP.
3. Documentar los endpoints y payloads de prueba para validación en **Postman** y **Swagger** (`/swagger-ui.html`).

---

## 6. Criterios de Verificación antes de Terminar

- [ ] `./mvnw clean compile` finaliza sin errores.
- [ ] Todos los endpoints de `/api/agent/**` exigen `Authorization: Bearer <JWT>` válido.
- [ ] Ningún usuario puede ejecutar un endpoint o tool de otro rol (devuelve `403 Forbidden`).
- [ ] La tabla `SPRING_AI_CHAT_MEMORY` persiste el historial aislado por `userId`.
- [ ] El modelo sigue configurado en `gemini-3.5-flash-lite` sin alterar el `.env`.
- [ ] Cada nuevo endpoint cuenta con un ejemplo JSON listo para probar en Postman.