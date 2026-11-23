# AGENTS.md — UNEXT Backend (`unext-web-services`)

> **Contrato Técnico Oficial y Fuente Única de Verdad** del backend de **UNEXT** para la integración con el frontend en Angular (`Unext-WebApplication`), mantenimiento del servicio y clientes externos MCP.
>
> Toda ruta, verbo HTTP, anotación de seguridad, parámetro, enumeración y propiedad JSON documentada aquí corresponde exactamente al código fuente actual en `src/main/java/com/nextworks/unextwebservices/` y `src/main/resources/`.

---

## 1. Arquitectura, Stack Tecnológico y Configuración Real

### 1.1 Stack y Versiones (`pom.xml`)
- **Artefacto:** `com.nextworks:unext-web-services:0.0.1-SNAPSHOT`
- **Lenguaje:** Java `21`
- **Framework:** Spring Boot `4.1.1` (`spring-boot-starter-parent`)
- **Módulos Spring Boot:** `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`
- **Base de Datos:** PostgreSQL (`org.postgresql:postgresql`)
- **Seguridad JWT:** `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (`0.12.5`)
- **Documentación OpenAPI / Swagger:** `org.springdoc:springdoc-openapi-starter-webmvc-ui` (`2.8.5`)
- **IA Generativa & MCP:** Spring AI `2.0.0` (`spring-ai-bom`):
  - `spring-ai-starter-model-openai` (endpoint compatible con Google Gemini)
  - `spring-ai-starter-model-chat-memory-repository-jdbc`
  - `spring-ai-starter-mcp-server-webmvc`
- **Utilidades:** Lombok, Spring Scheduling (`@EnableScheduling` en `UnextWebServicesApplication`)

### 1.2 Configuración del Servidor y Runtime (`application.properties`)
- **Puerto HTTP:** `8080` (`http://localhost:8080`)
- **Base de Datos PostgreSQL:**
  - URL: `jdbc:postgresql://localhost:5432/unext_db`
  - Usuario: `postgres` (`spring.jpa.hibernate.ddl-auto=update`)
- **Swagger / OpenAPI (`OpenApiConfig`):**
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - Especificación JSON: `http://localhost:8080/v3/api-docs`
  - Esquema de seguridad: `bearerAuth` (`HTTP Bearer`, formato `JWT`)
- **Variables desde `.env`:** `spring.config.import=optional:file:.env[.properties]`
- **Configuración de IA (Gemini):**
  - `spring.ai.openai.base-url=https://generativelanguage.googleapis.com/v1beta/openai/`
  - `spring.ai.openai.api-key=${GEMINI_API_KEY}`
  - `spring.ai.openai.chat.model=gemini-3.5-flash-lite`
  - `spring.ai.openai.chat.temperature=0.3`
  - `spring.ai.openai.chat.max-tokens=800`
  - `spring.ai.openai.chat.reasoning-effort=low`
- **Memoria Persistente del Agente:** tabla `SPRING_AI_CHAT_MEMORY` en `unext_db`, ventana de `20` mensajes (`MessageWindowChatMemory`), aislada por `user.getId().toString()`.
- **Servidor MCP (`spring-ai-starter-mcp-server-webmvc`):**
  - `spring.ai.mcp.server.enabled=true`
  - `spring.ai.mcp.server.name=unext-mcp-server`, `version=1.0.0`
  - `spring.ai.mcp.server.type=SYNC`
  - `spring.ai.mcp.server.protocol=STATELESS`
  - Endpoint HTTP MCP: `POST /mcp` (`spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp`)
- **Tarea Programada (`AccountCleanupService`):**
  - Cron `0 0 3 * * ?` (diario 03:00 AM): elimina perfiles y usuarios de `RECRUITER` e `INSTITUTION` con `isVerified = false` tras 7 días de antigüedad.

### 1.3 Estructura de Paquetes (`com.nextworks.unextwebservices`)
```text
src/main/java/com/nextworks/unextwebservices/
├── agent/
│   ├── mcp/          # UnextMcpProvider (@McpResource, @McpPrompt)
│   ├── rag/          # UnextRagAdvisor (CallAdvisor)
│   └── tools/        # AgentRuleGuard, PostulantTools, RecruiterTools, InstitutionTools
├── config/           # AgentConfig, McpServerConfig, OpenApiConfig
├── controller/       # 13 controladores REST
├── dto/              # Request/Response DTOs agrupados por dominio
├── entity/           # Entidades JPA + entity/enums/
├── repository/       # 19 interfaces JpaRepository
├── security/         # SecurityConfig, JwtAuthenticationFilter, JwtUtil
└── service/          # Lógica de negocio + RagContextService + AgentService
```

---

## 2. Seguridad, CORS y Reglas Críticas para el Frontend Angular

### 2.1 Autenticación JWT y Rutas Públicas (`SecurityConfig`, `JwtAuthenticationFilter`, `JwtUtil`)
- **Política de sesión:** `SessionCreationPolicy.STATELESS`, CSRF deshabilitado.
- **Expiración del token:** `jwt.expiration=86400000` (24 horas). El `subject` del JWT es el `email` del usuario.
- **Header requerido en toda ruta protegida:** `Authorization: Bearer <JWT>`
- **Errores del filtro JWT (`401 Unauthorized`):**
  - Token expirado $\rightarrow$ `{"error":"Token expirado. Vuelve a hacer login."}`
  - Token inválido/malformado $\rightarrow$ `{"error":"Token inválido. Copia el JWT completo, en una sola línea."}`
- **Rutas públicas (`permitAll`):**
  - `/api/auth/**`
  - `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/v3/api-docs`, `/swagger-resources/**`, `/webjars/**`
  - `/error`
- **Aislamiento de identidad:** Ningún endpoint protegido acepta `userId` en el body para suplantar al usuario autenticado; el backend extrae siempre `User` desde `Authentication` (`SecurityContextHolder` / JWT) y valida `@PreAuthorize` (`@EnableMethodSecurity`).
- **CORS permitido (`SecurityConfig`):**
  - Orígenes: `http://localhost:3000`, `http://localhost:5173`, `http://localhost:4200`
  - Métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
  - Headers: `Authorization`, `Content-Type`, `Accept`
  - `allowCredentials`: `true`

### 2.2 Reglas Críticas de Serialización Jackson / HTTP para `HttpClient` (Angular)
1. **Propiedades desconocidas prohibidas (`fail-on-unknown-properties=true`):**
   - Si Angular envía en el body JSON un campo que no existe exactamente en el `RequestDTO` destino, Spring Boot responderá **`400 Bad Request`**.
2. **Propiedades `boolean` primitivos vs `Boolean` en DTOs de respuesta:**
   - Lombok genera getters `isXxx()` para `boolean` primitivos, por lo que **Jackson elimina el prefijo `is`** en el JSON serializado:
     - `NetworkPersonCardDTO`: los campos Java `boolean isVerified` y `boolean isFollowing` se serializan en JSON como **`verified`** y **`following`** (y `boolean canConnect` como `canConnect`).
     - `FeedPostResponseDTO`: `boolean isImportant` se serializa en JSON como **`important`** (y `boolean likedByMe` como `likedByMe`), mientras que `Boolean isAuthorVerified` conserva **`isAuthorVerified`**.
   - Todos los campos declarados con el wrapper **`Boolean`** conservan el prefijo `is` en JSON (`isProfileCompleted`, `isVerified`, `isInstitutionVerified`, `isValidated`, `isRead`, `isActive`, `isTopSkill`, `isValidatedByInstitution`, `isAuthorVerified`).
3. **Endpoints que devuelven texto plano (`text/plain`):**
   - Los endpoints con retorno `ResponseEntity<String>` devuelven texto plano, NO JSON. En Angular `HttpClient` deben consumirse con `{ responseType: 'text' }`.
4. **Endpoints `204 No Content`:**
   - Devuelven cuerpo vacío (`ResponseEntity<Void>`).

---

## 3. Enumeraciones Oficiales (`entity/enums/`)

| Enum | Valores exactos |
| :--- | :--- |
| `Role` | `POSTULANT`, `RECRUITER`, `INSTITUTION` |
| `JobModality` | `REMOTE`, `HYBRID`, `ONSITE` |
| `ExperienceLevel` | `TRAINEE`, `JUNIOR`, `SEMI_SENIOR`, `SENIOR` |
| `ApplicationStatus` | `RECEIVED`, `UNDER_REVIEW`, `ACCEPTED`, `REJECTED` |
| `AgreementStatus` | `PENDING`, `APPROVED`, `REJECTED` |
| `ValidationStatus` | `PENDING`, `APPROVED`, `REJECTED` |
| `CompatibilityLabel` | `NA`, `BAJA`, `MEDIA`, `ALTA` |
| `ConnectionStatus` | `PENDING`, `ACCEPTED`, `REJECTED` |
| `ChatContext` | `JOB_APPLICATION`, `DIRECT_MESSAGE`, `INSTITUTIONAL`, `GENERAL` |
| `ReviewStatus` | `DRAFT`, `PUBLISHED`, `HIDDEN` |
| `ReportStatus` | `PENDING`, `CANCELLED` |

---

## 4. Inventario Completo de Endpoints REST y Contratos DTO

> **Nota de estructura de controladores:** En el backend existen exactamente **13 controladores**. Los endpoints de directorios públicos están integrados en `ProfileController` (no existe `DirectoryController`) y el muro social está en `FeedPostController` (no `FeedController`).

---

### 4.1 `AuthController` — `/api/auth` (Público)

| Método | Ruta | Seguridad | Retorno |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Público | `201 Created` — `AuthResponseDTO` |
| `POST` | `/api/auth/login` | Público | `200 OK` — `AuthResponseDTO` |

- **`RegisterRequestDTO` (Entrada `POST /register`):**
  - `email`: `String` (`@NotBlank`, `@Email`)
  - `password`: `String` (`@NotBlank`, `@Size(min = 6)`)
  - `role`: `Role` (`@NotNull`: `POSTULANT` | `RECRUITER` | `INSTITUTION`)
- **`LoginRequestDTO` (Entrada `POST /login`):**
  - `email`: `String` (`@NotBlank`, `@Email`)
  - `password`: `String` (`@NotBlank`)
- **`AuthResponseDTO` (Salida):**
  ```json
  {
    "token": "string (JWT)",
    "userId": "UUID",
    "role": "POSTULANT | RECRUITER | INSTITUTION",
    "isProfileCompleted": false
  }
  ```

---

### 4.2 `ProfileController` — `/api/profiles`

#### A. Creación, Verificación Organizacional, Lectura y Edición Propia

| Método | Ruta | Rol (`@PreAuthorize`) | Entrada | Salida |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/profiles/postulant/me` | `POSTULANT` | `PostulantProfileRequestDTO` | `200` `String` (`text/plain`) |
| `POST` | `/api/profiles/recruiter/me` | `RECRUITER` | `RecruiterProfileRequestDTO` | `200` `String` (`text/plain`) |
| `POST` | `/api/profiles/institution/me` | `INSTITUTION` | `InstitutionProfileRequestDTO` | `200` `String` (`text/plain`) |
| `POST` | `/api/profiles/organization/verify` | `RECRUITER`, `INSTITUTION` | `OrganizationVerifyRequestDTO` | `200` `String` (`text/plain`) |
| `POST` | `/api/profiles/organization/resend-code` | `RECRUITER`, `INSTITUTION` | *(sin body)* | `200` `{"message": "...", "code": "123456"}` |
| `GET` | `/api/profiles/postulant/me` | `POSTULANT` | — | `PostulantProfileResponseDTO` |
| `PUT` | `/api/profiles/postulant/me` | `POSTULANT` | `PostulantProfileUpdateDTO` | `PostulantProfileResponseDTO` |
| `GET` | `/api/profiles/recruiter/me` | `RECRUITER` | — | `RecruiterProfileResponseDTO` |
| `PUT` | `/api/profiles/recruiter/me` | `RECRUITER` | `RecruiterProfileUpdateDTO` | `RecruiterProfileResponseDTO` |
| `GET` | `/api/profiles/institution/me` | `INSTITUTION` | — | `InstitutionProfileResponseDTO` |
| `PUT` | `/api/profiles/institution/me` | `INSTITUTION` | `InstitutionProfileUpdateDTO` | `InstitutionProfileResponseDTO` |

#### B. Habilidades del Postulante

| Método | Ruta | Rol (`@PreAuthorize`) | Entrada | Salida |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/profiles/postulant/me/skills` | `POSTULANT` | `StudentSkillRequestDTO` | `StudentSkillResponseDTO` |
| `GET` | `/api/profiles/postulant/me/skills` | `POSTULANT` | — | `List<StudentSkillResponseDTO>` |
| `PUT` | `/api/profiles/postulant/me/skills/{skillId}` | `POSTULANT` | `StudentSkillUpdateDTO` | `StudentSkillResponseDTO` |
| `DELETE` | `/api/profiles/postulant/me/skills/{skillId}` | `POSTULANT` | — | `204 No Content` |

#### C. Directorios Públicos (Para Usuarios Autenticados)

| Método | Ruta | Roles (`@PreAuthorize`) | Salida |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/profiles/postulants/directory` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `List<PostulantDirectoryResponseDTO>` |
| `GET` | `/api/profiles/postulants/directory/{postulantId}` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `PostulantProfileResponseDTO` |
| `GET` | `/api/profiles/directory` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `List<RecruiterDirectoryResponseDTO>` |
| `GET` | `/api/profiles/recruiters/directory/{recruiterId}` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `RecruiterProfileResponseDTO` |
| `GET` | `/api/profiles/recruiters/directory/{recruiterId}/jobs` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `List<JobOfferResponseDTO>` |
| `GET` | `/api/profiles/institution/directory` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `List<InstitutionDirectoryResponseDTO>` |
| `GET` | `/api/profiles/institutions/directory/{institutionId}` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `InstitutionProfileResponseDTO` |

#### Contratos DTO de `ProfileController`

- **`PostulantProfileRequestDTO`:**
  - `firstName`: `String` (`@NotBlank`, `@Size(max = 60)`)
  - `lastName`: `String` (`@NotBlank`, `@Size(max = 60)`)
  - `headline`: `String` (`@Size(max = 120)`)
  - `bio`: `String` (`@Size(max = 1000)`)
  - `bannerText`: `String` (`@Size(max = 35)`)
  - `avatarUrl`, `bannerUrl`, `cvUrl`, `cvFileName`, `cvFileSize`, `studentCode`, `career`: `String`
  - `currentCycle`: `Integer`
- **`PostulantProfileUpdateDTO`:**
  - `firstName` (`@Size(max = 60)`), `lastName` (`@Size(max = 60)`), `headline` (`@Size(max = 120)`), `bio` (`@Size(max = 1000)`), `bannerText` (`@Size(max = 35)`), `avatarUrl`, `bannerUrl`, `cvUrl`, `cvFileName`, `cvFileSize`, `career`: `String`
  - `currentCycle`: `Integer`
- **`PostulantProfileResponseDTO`:**
  - `id` (`UUID`), `userId` (`UUID`), `firstName`, `lastName`, `studentCode`, `career`, `currentCycle` (`Integer`), `institutionalEmail`, `academicStatus`, `academicEvidenceUrl`, `academicEvidenceName`, `avatarUrl`, `bannerUrl`, `bannerText`, `cvUrl`, `cvFileName`, `cvFileSize`, `headline`, `bio`, `hasUniversityBase` (`Boolean`), `isInstitutionVerified` (`Boolean`), `institutionProfile` (`UUID`), `institutionName`, `institutionLogoUrl`, `skills` (`List<StudentSkillResponseDTO>`)
- **`RecruiterProfileRequestDTO`:**
  - `companyName`: `String` (`@NotBlank`, `@Size(max = 120)`)
  - `ruc`: `String` (`@NotBlank`, debe cumplir regex `^(10|20)\d{9}$`)
  - `industry`: `String` (`@Size(max = 100)`)
  - `headline`: `String` (`@Size(max = 120)`)
  - `websiteUrl`: `String`
  - `description`: `String` (`@Size(max = 1500)`)
  - `bannerText`: `String` (`@Size(max = 35)`)
  - `companyLogoUrl`, `bannerUrl`: `String`
- **`RecruiterProfileUpdateDTO`:**
  - `companyName` (`@Size(max = 120)`), `industry` (`@Size(max = 100)`), `headline` (`@Size(max = 120)`), `websiteUrl`, `description` (`@Size(max = 1500)`), `bannerText` (`@Size(max = 35)`), `companyLogoUrl`, `bannerUrl`: `String`
- **`RecruiterProfileResponseDTO`:**
  - `id` (`UUID`), `userId` (`UUID`), `companyName`, `ruc`, `industry`, `headline`, `websiteUrl`, `description`, `companyLogoUrl`, `bannerUrl`, `bannerText`, `isValidated` (`Boolean`), `isVerified` (`Boolean`), `validationsCount` (`Integer`), `pendingVerificationCode` (`String` — `null` si ya está verificada o si se consulta vía directorio público)
- **`InstitutionProfileRequestDTO`:**
  - `institutionName`: `String` (`@NotBlank`)
  - `domain`: `String` (`@NotBlank`)
- **`InstitutionProfileUpdateDTO`:**
  - `name`, `domain`, `logoUrl`, `bannerUrl`, `bannerText`, `description`: `String`
- **`InstitutionProfileResponseDTO`:**
  - `id` (`UUID`), `userId` (`UUID`), `name`, `domain`, `logoUrl`, `bannerUrl`, `bannerText`, `description`, `isVerified` (`Boolean`), `pendingVerificationCode` (`String` — `null` si verificada o vía directorio)
- **`OrganizationVerifyRequestDTO`:**
  - `code`: `String` (`@NotBlank`, `@Size(min = 6, max = 6)`)
- **`StudentSkillRequestDTO`:**
  - `name`: `String` (`@NotBlank`)
  - `masteryLevel`: `String` (`@NotBlank`, ej. `BASICO` / `INTERMEDIO` / `AVANZADO`)
  - `isTopSkill`: `Boolean` *(máximo 3 por postulante)*
- **`StudentSkillUpdateDTO`:**
  - `name`, `masteryLevel`: `String`, `isTopSkill`: `Boolean`
- **`StudentSkillResponseDTO`:**
  - `id` (`UUID`), `name`, `masteryLevel`, `isTopSkill` (`Boolean`), `isValidatedByInstitution` (`Boolean`), `validatedInstitutionName`, `validatedInstitutionLogoUrl`
- **DTOs de Directorio:**
  - `PostulantDirectoryResponseDTO`: `id` (`UUID`), `userId` (`UUID`), `firstName`, `lastName`, `career`, `headline`, `avatarUrl`, `isInstitutionVerified` (`Boolean`)
  - `RecruiterDirectoryResponseDTO`: `id` (`UUID`), `userId` (`UUID`), `companyName`, `industry`, `companyLogoUrl`
  - `InstitutionDirectoryResponseDTO`: `id` (`UUID`), `userId` (`UUID`), `name`, `domain`, `logoUrl`, `description`

---

### 4.3 `JobController` — `/api/jobs`

| Método | Ruta | Query Params / Path | Roles (`@PreAuthorize`) | Salida |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/jobs` | `?keyword=&modality=&experience=` *(todos opcionales)* | `POSTULANT`, `INSTITUTION`, `RECRUITER` | `List<JobOfferResponseDTO>` |
| `GET` | `/api/jobs/skills/suggestions` | `?q=` *(opcional)* | `POSTULANT`, `RECRUITER` | `List<String>` |
| `GET` | `/api/jobs/recommendations` | — *(afinidad ≥ 80%)* | `POSTULANT` | `List<JobRecommendationResponseDTO>` |
| `POST` | `/api/jobs/{jobId}/not-interested` | `jobId` (`UUID`) | `POSTULANT` | `String` (`text/plain`) |
| `POST` | `/api/jobs/{jobId}/apply` | `jobId` (`UUID`) | `POSTULANT` | `ApplicationResponseDTO` |
| `GET` | `/api/jobs/applications/me` | — | `POSTULANT` | `List<ApplicationResponseDTO>` |

- **`JobOfferResponseDTO`:** `id` (`UUID`), `companyName`, `title`, `description`, `requirements`, `requiredSkills` (`List<String>`), `location`, `modality` (`JobModality`), `experienceLevel` (`ExperienceLevel`), `minSalary` (`BigDecimal`), `maxSalary` (`BigDecimal`), `isActive` (`Boolean`), `createdAt` (`LocalDateTime`)
- **`JobRecommendationResponseDTO`:** `id` (`UUID`), `companyName`, `title`, `description`, `location`, `modality` (`JobModality`), `experienceLevel` (`ExperienceLevel`), `minSalary` (`BigDecimal`), `maxSalary` (`BigDecimal`), `requiredSkills` (`List<String>`), `matchedSkills` (`List<String>`), `compatibilityPercent` (`Integer`), `compatibilityLabel` (`CompatibilityLabel`), `createdAt` (`LocalDateTime`)
- **`ApplicationResponseDTO`:** `id` (`UUID`), `jobTitle`, `companyName`, `status` (`ApplicationStatus`), `appliedAt` (`LocalDateTime`)

---

### 4.4 `RecruiterController` — `/api/recruiter` (Rol exclusivo: `RECRUITER`)

| Método | Ruta | Params / Body | Salida |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/recruiter/jobs` | `JobOfferRequestDTO` | `JobOfferResponseDTO` |
| `GET` | `/api/recruiter/jobs` | — | `List<JobOfferResponseDTO>` |
| `PUT` | `/api/recruiter/jobs/{jobId}` | `jobId` (`UUID`), `JobOfferUpdateDTO` | `JobOfferResponseDTO` |
| `DELETE` | `/api/recruiter/jobs/{jobId}` | `jobId` (`UUID`) *(cierra vacante: `isActive=false`)* | `JobOfferResponseDTO` |
| `GET` | `/api/recruiter/jobs/{jobId}/applications` | `jobId` (`UUID`), opcionales: `?career=&minCycle=&skill=&minRating=&minReviews=` | `List<RecruiterApplicationResponseDTO>` |
| `PATCH` | `/api/recruiter/applications/{applicationId}/status` | `applicationId` (`UUID`), `@RequestParam ApplicationStatus status` | `String` (`text/plain`) |
| `POST` | `/api/recruiter/agreements` | `AgreementCreateRequestDTO` | `AgreementResponseDTO` |
| `GET` | `/api/recruiter/agreements` | — | `List<AgreementResponseDTO>` |

- **`JobOfferRequestDTO`:**
  - `title` (`@NotBlank`), `description` (`@NotBlank`), `requirements` (`@NotBlank`), `requiredSkills` (`List<String>`, `@NotEmpty`), `location` (`String`), `modality` (`JobModality`, `@NotNull`), `experienceLevel` (`ExperienceLevel`, `@NotNull`), `minSalary` (`BigDecimal`), `maxSalary` (`BigDecimal`)
- **`JobOfferUpdateDTO`:**
  - `companyName` (`String`, aceptado en schema aunque no editable), `title`, `description`, `requirements`, `requiredSkills` (`List<String>`), `location`, `modality` (`JobModality`), `experienceLevel` (`ExperienceLevel`), `minSalary` (`BigDecimal`), `maxSalary` (`BigDecimal`)
- **`RecruiterApplicationResponseDTO`:**
  - `id` (`UUID`), `postulantProfileId` (`UUID`), `postulantUserId` (`UUID`), `firstName`, `lastName`, `avatarUrl`, `cvUrl`, `cvFileName`, `career`, `currentCycle` (`Integer`), `headline`, `isInstitutionVerified` (`Boolean`), `skills` (`List<String>`), `status` (`ApplicationStatus`), `appliedAt` (`LocalDateTime`), `compatibilityPercent` (`Integer`), `compatibilityLabel` (`CompatibilityLabel`), `matchingHint`, `averageStars` (`Double`), `reviewCount` (`Long`)
- **`AgreementCreateRequestDTO`:**
  - `postulantId` (`UUID`, `@NotNull`), `jobApplicationId` (`UUID`, opcional — si se envía exige vacante `TRAINEE`), `title` (`@NotBlank`), `startDate` (`LocalDate`, `@NotNull`), `endDate` (`LocalDate`, `@NotNull`), `weeklyHours` (`Integer`, `@NotNull`), `documentUrl` (`@NotBlank`), `observations` (`String`)
- **`AgreementResponseDTO`:**
  - `id` (`UUID`), `jobApplicationId` (`UUID`), `title`, `companyName`, `studentName`, `startDate` (`LocalDate`), `endDate` (`LocalDate`), `weeklyHours` (`Integer`), `documentUrl`, `status` (`AgreementStatus`), `observations`, `reviewedAt` (`LocalDateTime`), `createdAt` (`LocalDateTime`)

---

### 4.5 `ValidationController` — `/api/validations` (Rol exclusivo: `POSTULANT`)

| Método | Ruta | Entrada | Salida |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/validations/request` | `ValidationRequestDTO` | `ValidationResponseDTO` |
| `GET` | `/api/validations/me` | — | `List<ValidationResponseDTO>` |
| `POST` | `/api/validations/me/academic-verification` | `AcademicVerificationRequestDTO` | `String` (`text/plain`) |
| `PATCH` | `/api/validations/me/institution/{institutionId}` | `institutionId` (`UUID`) | `String` (`text/plain`) |

- **`ValidationRequestDTO`:** `institutionProfileId` (`UUID`, `@NotNull`), `technicalSkillId` (`UUID`, `@NotNull`), `evidenceUrl` (`String`, `@NotBlank`)
- **`ValidationResponseDTO`:** `id` (`UUID`), `technicalSkillId` (`UUID`), `institutionName`, `institutionLogoUrl`, `technicalSkillName`, `evidenceUrl`, `status` (`ValidationStatus`)
- **`AcademicVerificationRequestDTO`:**
  - `institutionProfileId` (`UUID`, `@NotNull`), `academicStatus` (`String`, `@NotBlank`: `"STUDENT"` o `"GRADUATE"`)
  - Para `"STUDENT"`: `institutionalEmail`, `studentCode`, `career`, `currentCycle` (`Integer`)
  - Para `"GRADUATE"`: `academicEvidenceUrl`, `academicEvidenceName`, `career` (opcional)

---

### 4.6 `InstitutionController` — `/api/institution` (Rol exclusivo: `INSTITUTION`)

| Método | Ruta | Params / Body | Salida |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/institution/students/pending` | — | `List<PendingStudentResponseDTO>` |
| `PATCH` | `/api/institution/students/{postulantId}/verify` | `postulantId` (`UUID`), `@RequestParam ValidationStatus status` (`APPROVED` \| `REJECTED`) | `String` (`text/plain`) |
| `POST` | `/api/institution/companies/{recruiterId}/endorse` | `recruiterId` (`UUID`) | `String` (`text/plain`) |
| `DELETE` | `/api/institution/companies/{recruiterId}/endorse` | `recruiterId` (`UUID`), `RevokeEndorsementRequestDTO` (`reason` `@NotBlank`) | `String` (`text/plain`) |
| `GET` | `/api/institution/companies/endorsed` | — | `List<EndorsedCompanyResponseDTO>` |
| `GET` | `/api/institution/agreements` | `?status=` (`AgreementStatus`, opcional) | `List<AgreementResponseDTO>` |
| `PATCH` | `/api/institution/agreements/{agreementId}/status` | `agreementId` (`UUID`), `AgreementUpdateRequestDTO` (`status` `@NotNull`, `observations`) | `AgreementResponseDTO` |
| `GET` | `/api/institution/dashboard/stats` | `?career=` (opcional) | `DashboardStatsResponseDTO` |
| `GET` | `/api/institution/validations/pending` | — | `List<PendingValidationResponseDTO>` |
| `PATCH` | `/api/institution/validations/{validationId}/status` | `validationId` (`UUID`), `ValidationUpdateDTO` (`status` `@NotNull` `String`, `observations`) | `String` (`text/plain`) |

- **DTOs específicos de `InstitutionController`:**
  - `PendingStudentResponseDTO`: `postulantId` (`UUID`), `firstName`, `lastName`, `email`, `isVerified` (`Boolean`)
  - `EndorsedCompanyResponseDTO`: `recruiterId` (`UUID`), `companyName`, `endorsedAt` (`LocalDateTime`)
  - `PendingValidationResponseDTO`: `validationId` (`UUID`), `studentName`, `skillName`, `evidenceUrl`, `status` (`String`)
  - `RevokeEndorsementRequestDTO`: `reason` (`String`, `@NotBlank`)
  - `AgreementUpdateRequestDTO`: `status` (`AgreementStatus`, `@NotNull`), `observations` (`String`)
  - `ValidationUpdateDTO`: `status` (`String`, `@NotNull`: `"APPROVED"` \| `"REJECTED"`), `observations` (`String`)
  - `DashboardStatsResponseDTO`: `totalVerifiedStudents` (`long`), `totalHiredStudents` (`long`), `totalActiveAgreements` (`long`), `totalEndorsedCompanies` (`long`), `employabilityRate` (`double`), `career` (`String`)

---

### 4.7 `DashboardController` — `/api/institution/dashboard` (Rol exclusivo: `INSTITUTION`)

| Método | Ruta | Query Params | Salida |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/institution/dashboard/summary` | `?career=` (opcional) | `DashboardSummaryResponseDTO` |
| `GET` | `/api/institution/dashboard/top-companies` | `?limit=5` (`1..50`), `?career=` (opcional) | `TopCompaniesResponseDTO` |
| `GET` | `/api/institution/dashboard/demand-by-career` | — | `DemandByCareerResponseDTO` |

- **`DashboardSummaryResponseDTO`:** `career`, `totalVerifiedStudents` (`long`), `totalHiredStudents` (`long`), `totalActiveAgreements` (`long`), `totalEndorsedCompanies` (`long`), `employabilityRate` (`double`), `message` (`String`)
- **`TopCompaniesResponseDTO`:** `career`, `companies` (`List<TopCompanyDTO>`: `recruiterId` `UUID`, `companyName`, `hiredStudents` `Long`, `isValidated` `Boolean`), `message`
- **`DemandByCareerResponseDTO`:** `careers` (`List<CareerDemandDTO>`: `career`, `applications` `Long`, `accepted` `Long`), `message`

---

### 4.8 `ReviewController` — `/api/reviews`

| Método | Ruta | Rol (`@PreAuthorize`) | Params / Entrada | Salida |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/reviews` | `RECRUITER` | `ReviewRequestDTO` | `ReviewResponseDTO` |
| `GET` | `/api/reviews/me` | `POSTULANT` | `?stars=` (`Integer`, opcional) | `ReputationResponseDTO` |
| `POST` | `/api/reviews/{reviewId}/reports` | `POSTULANT`, `RECRUITER` | `reviewId` (`UUID`), `ReportRequestDTO` | `ReportResponseDTO` |
| `DELETE` | `/api/reviews/reports/{reportId}` | `POSTULANT`, `RECRUITER` | `reportId` (`UUID`) | `String` (`text/plain`) |

- **`ReviewRequestDTO`:** `agreementId` (`UUID`, `@NotNull`), `stars` (`Integer`, `@NotNull @Min(1) @Max(5)`), `comment` (`String`, `@NotBlank`), `publish` (`Boolean`)
- **`ReviewResponseDTO`:** `id` (`UUID`), `agreementId` (`UUID`), `companyName`, `postulantName`, `stars` (`Integer`), `comment`, `status` (`ReviewStatus`), `createdAt` (`LocalDateTime`)
- **`ReputationResponseDTO`:** `averageStars` (`Double`), `totalReviews` (`Integer`), `reviews` (`List<ReviewResponseDTO>`)
- **`ReportRequestDTO`:** `reason` (`String`, `@NotBlank`)
- **`ReportResponseDTO`:** `id` (`UUID`), `reviewId` (`UUID`), `status` (`ReportStatus`), `message`

---

### 4.9 `ChatController` — `/api/messages` (Roles: `POSTULANT`, `INSTITUTION`, `RECRUITER`)

| Método | Ruta | Params / Entrada | Salida |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/messages/send` | `MessageRequestDTO` | `MessageResponseDTO` |
| `GET` | `/api/messages/inbox` | — | `List<MessageResponseDTO>` |
| `GET` | `/api/messages/history/{receiverId}` | `receiverId` (`UUID`) | `List<MessageResponseDTO>` |

- **`MessageRequestDTO`:** `receiverId` (`UUID`, `@NotNull`), `content` (`String`, `@NotBlank`), `jobApplicationId` (`UUID`, opcional)
- **`MessageResponseDTO`:** `id` (`UUID`), `senderId` (`UUID`), `receiverId` (`UUID`), `content`, `createdAt` (`LocalDateTime`), `isRead` (`Boolean`), `contextType` (`ChatContext`)

---

### 4.10 `NotificationController` — `/api/notifications` (Roles: `POSTULANT`, `INSTITUTION`, `RECRUITER`)

| Método | Ruta | Path Param | Salida |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/notifications` | — | `List<NotificationResponseDTO>` |
| `PATCH` | `/api/notifications/{notificationId}/read` | `notificationId` (`UUID`) | `NotificationResponseDTO` |
| `PATCH` | `/api/notifications/read-all` | — | `204 No Content` |
| `DELETE` | `/api/notifications/{notificationId}` | `notificationId` (`UUID`) | `204 No Content` |

- **`NotificationResponseDTO`:** `id` (`UUID`), `title`, `content`, `isRead` (`Boolean`), `createdAt` (`LocalDateTime`)

---

### 4.11 `FeedPostController` — `/api/feed` (Roles: `POSTULANT`, `RECRUITER`, `INSTITUTION`)

| Método | Ruta | Params / Entrada | Salida |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/feed` | `FeedPostRequestDTO` | `FeedPostResponseDTO` |
| `GET` | `/api/feed` | — | `List<FeedPostResponseDTO>` |
| `GET` | `/api/feed/user/{authorUserId}` | `authorUserId` (`UUID`) | `List<FeedPostResponseDTO>` |
| `GET` | `/api/feed/user/{authorUserId}/important` | `authorUserId` (`UUID`) | `List<FeedPostResponseDTO>` |
| `POST` | `/api/feed/{postId}/like` | `postId` (`UUID`) | `FeedPostResponseDTO` |
| `PATCH` | `/api/feed/{postId}/important` | `postId` (`UUID`) | `FeedPostResponseDTO` |
| `DELETE` | `/api/feed/{postId}` | `postId` (`UUID`) | `String` (`text/plain`) |
| `POST` | `/api/feed/{postId}/comments` | `postId` (`UUID`), `FeedCommentRequestDTO` | `FeedCommentResponseDTO` |
| `GET` | `/api/feed/{postId}/comments` | `postId` (`UUID`) | `List<FeedCommentResponseDTO>` |
| `DELETE` | `/api/feed/comments/{commentId}` | `commentId` (`UUID`) | `String` (`text/plain`) |

- **`FeedPostRequestDTO`:** `content` (`String`, `@NotBlank`), `mediaUrls` (`List<String>`), `isImportant` (`Boolean`)
- **`FeedPostResponseDTO`:**
  - `id` (`UUID`), `authorUserId` (`UUID`), `authorProfileId` (`UUID`), `authorRole` (`String`), `authorName`, `authorHeadline`, `authorAvatarUrl`, `isAuthorVerified` (`Boolean`), `content`, `mediaUrls` (`List<String>`), `likesCount` (`int`), `commentsCount` (`long`), `likedByMe` (`boolean`), **`important`** (`boolean` en Java: `isImportant`), `expiresAt` (`LocalDateTime`), `createdAt` (`LocalDateTime`)
- **`FeedCommentRequestDTO`:** `content` (`String`, `@NotBlank`)
- **`FeedCommentResponseDTO`:** `id` (`UUID`), `postId` (`UUID`), `authorUserId` (`UUID`), `authorProfileId` (`UUID`), `authorRole` (`String`), `authorName`, `authorHeadline`, `authorAvatarUrl`, `isAuthorVerified` (`Boolean`), `content`, `createdAt` (`LocalDateTime`)

---

### 4.12 `ConnectionController` — `/api/connections` (Roles: `POSTULANT`, `RECRUITER`, `INSTITUTION`)

| Método | Ruta | Path Param | Salida |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/connections/status/{targetUserId}` | `targetUserId` (`UUID`) | `ConnectionStatusResponseDTO` |
| `POST` | `/api/connections/request/{targetUserId}` | `targetUserId` (`UUID`) | `ConnectionStatusResponseDTO` |
| `POST` | `/api/connections/{connectionId}/accept` | `connectionId` (`UUID`) | `ConnectionStatusResponseDTO` |
| `DELETE` | `/api/connections/user/{targetUserId}` | `targetUserId` (`UUID`) | `204 No Content` |
| `GET` | `/api/connections/overview` | — | `NetworkOverviewDTO` |
| `POST` | `/api/connections/follow/{targetUserId}` | `targetUserId` (`UUID`) | `{"following": boolean}` |

- **`ConnectionStatusResponseDTO`:** `connectionId` (`UUID`), `targetUserId` (`UUID`), `status` (`"CONNECTED"` \| `"PENDING_SENT"` \| `"PENDING_RECEIVED"` \| `"NONE"`), `canConnect` (`boolean`), `restrictionReason` (`String`)
- **`NetworkOverviewDTO`:** `connectionsCount` (`long`), `receivedInvitationsCount` (`long`), `sentInvitationsCount` (`long`), `followingCount` (`long`), `receivedInvitations`, `sentInvitations`, `connections`, `following`, `recommendations` (`List<NetworkPersonCardDTO>`)
- **`NetworkPersonCardDTO`:** `connectionId` (`UUID`), `userId` (`UUID`), `profileId` (`UUID`), `role` (`String`), `name`, `headline`, `subtitle`, `avatarUrl`, `bannerUrl`, `bannerText`, **`verified`** (`boolean` en Java: `isVerified`), `connectionStatus`, **`following`** (`boolean` en Java: `isFollowing`), `canConnect` (`boolean`), `restrictionReason`, `createdAt` (`LocalDateTime`)

---

### 4.13 `AgentController` — `/api/agent` (Endpoints REST de IA Generativa para Angular)

| Método | Ruta | Rol (`@PreAuthorize`) | Request DTO | Response DTO |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/agent/chat` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `ChatRequestDTO` | `ChatResponseDTO` |
| `POST` | `/api/agent/suggest` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `ChatRequestDTO` | `JobSuggestionListDTO` |
| `POST` | `/api/agent/optimize-bio` | `POSTULANT`, `RECRUITER`, `INSTITUTION` | `BioOptimizeRequestDTO` | `BioOptimizeResponseDTO` |
| `POST` | `/api/agent/postulant/talent-analysis` | `POSTULANT` | `TalentAnalysisRequestDTO` *(opcional)* | `TalentAnalysisResponseDTO` |
| `POST` | `/api/agent/recruiter/design-job` | `RECRUITER` | `JobDesignRequestDTO` | `JobBlueprintSuggestionDTO` |
| `POST` | `/api/agent/institution/employability-insights` | `INSTITUTION` | `EmployabilityInsightsRequestDTO` *(opcional)* | `EmployabilityInsightsResponseDTO` |

#### Contratos DTO exactos de `dto/agent/` (Java `record`)

1. **`ChatRequestDTO`:**
   - `message`: `String` (`@NotBlank`, `@Size(max = 2000)`)
2. **`ChatResponseDTO`:**
   - `reply`: `String`
3. **`BioOptimizeRequestDTO`:**
   - `bio`: `String` (`@NotBlank`, `@Size(max = 2000)`, `@JsonAlias({"draftBio", "description", "content", "message"})`)
4. **`BioOptimizeResponseDTO`:**
   - `role`: `String`, `originalBio`: `String`, `optimizedBio`: `String`, `keyImprovements`: `List<String>`
5. **`TalentAnalysisRequestDTO`** *(body opcional; acepta `{}` o sin body)*:
   - `targetRole`: `String` (`@Size(max = 300)`, `@JsonAlias({"careerGoal", "message", "keyword"})`)
6. **`TalentAnalysisResponseDTO`:**
   - `postulantName`: `String`, `career`: `String`, `currentSkills`: `List<String>`, `matchedDemandSkills`: `List<String>`, `missingKeySkills`: `List<String>`, `activeVacanciesAnalyzed`: `int`, `marketCompatibilitySummary`: `String`, `actionPlan`: `List<String>`
7. **`JobDesignRequestDTO`:**
   - `prompt`: `String` (`@NotBlank`, `@Size(max = 1000)`, `@JsonAlias({"roleIdea", "title", "message", "idea"})`)
8. **`JobBlueprintSuggestionDTO`:**
   - `roleTitle`: `String` (`@NotBlank`), `suggestedModality`: `String` (`@NotBlank`), `suggestedExperienceLevel`: `String` (`@NotBlank`), `estimatedSalaryRangeSoles`: `String`, `recommendedSkills`: `List<String>`, `optimizedDescription`: `String`
9. **`JobSuggestionListDTO`:**
   - `suggestions`: `List<JobBlueprintSuggestionDTO>`
10. **`EmployabilityInsightsRequestDTO`** *(body opcional; acepta `{}` o sin body)*:
    - `career`: `String` (`@Size(max = 150)`)
    - `focusQuestion`: `String` (`@Size(max = 1000)`, `@JsonAlias({"question", "message"})`)
11. **`EmployabilityInsightsResponseDTO`:**
    - `institutionName`: `String`, `careerFilter`: `String`, `totalVerifiedStudents`: `long`, `totalHiredStudents`: `long`, `totalActiveAgreements`: `long`, `totalEndorsedCompanies`: `long`, `employabilityRate`: `double`, `executiveSummary`: `String`, `keyFindings`: `List<String>`, `strategicRecommendations`: `List<String>`

---

## 5. Detalle Interno de la Capa de Inteligencia Artificial Generativa

### 5.1 Componentes y Flujo de Ejecución
1. **`src/main/resources/prompts/system-prompt.md`:** Prompt maestro en español (contexto laboral peruano, salarios en `S/.`) con marcadores dinámicos `{today}`, `{name}` y `{role}`.
2. **`AgentConfig`:** Define `chatMemory` (`MessageWindowChatMemory` con `JdbcChatMemoryRepository`, máx. `20` mensajes) y dos beans de `ChatClient`:
   - `chatClient` (`@Primary`): configurado con `MessageChatMemoryAdvisor` + `UnextRagAdvisor`. Lo usan `/chat` y `/suggest`.
   - `statelessChatClient`: configurado únicamente con `UnextRagAdvisor` (sin `MessageChatMemoryAdvisor`) para los 4 casos de uso especializados (`/optimize-bio`, `/postulant/talent-analysis`, `/recruiter/design-job`, `/institution/employability-insights`), evitando contaminar la memoria conversacional del usuario en `SPRING_AI_CHAT_MEMORY`.
3. **Capa RAG (`RagContextService` + `UnextRagAdvisor`):**
   - Antes de enviar el prompt a Gemini, `UnextRagAdvisor` (`CallAdvisor`, orden `0`) invoca `RagContextService.retrieveContext(user, userQuery)` e inyecta en el `SystemMessage`: reglas de negocio oficiales de UNEXT, el perfil y habilidades/vacantes/métricas reales del usuario autenticado desde `unext_db`, y hasta 5 vacantes activas relevantes filtradas por palabras clave más las 15 habilidades más demandadas.
   - Para `/postulant/talent-analysis` y `/institution/employability-insights`, `RagContextService` calcula además `TalentSnapshot` e `InstitutionSnapshot` de forma determinística en transacciones `readOnly = true` antes de invocar al modelo, asegurando que listas, conteos y porcentajes numéricos provengan al 100% de PostgreSQL.
4. **Capa de Tools (`agent/tools/`):**
   - `AgentRuleGuard`: valida JWT/rol y enlaza el `User` autenticado en un `ThreadLocal` durante la ejecución de la tool (con respaldo en `SecurityContextHolder`).
   - `AgentService.chat(...)` entrega dinámicamente al modelo **únicamente** el bean de herramientas correspondiente al rol del usuario autenticado:
     - `POSTULANT` $\rightarrow$ `PostulantTools`: `obtenerMisHabilidadesPostulante`, `listarVacantesActivasDisponibles`, `consultarMisPostulaciones`.
     - `RECRUITER` $\rightarrow$ `RecruiterTools`: `consultarMisVacantesEmpresa`, `consultarConveniosEmpresa`.
     - `INSTITUTION` $\rightarrow$ `InstitutionTools`: `consultarEstadisticasEmpleabilidadInstitucion`, `consultarPendientesInstitucion`.
   - Además, `AgentService` reintenta automáticamente con `gemini-3.5-flash-lite` si la API devuelve un error `503`.

### 5.2 Separación Explícita: Frontend Angular vs Clientes MCP Externos

#### A. Endpoints para el Frontend Angular (`Unext-WebApplication`)
El frontend en Angular **únicamente** debe consumir los endpoints REST bajo `/api/agent/**` con `Authorization: Bearer <JWT>` y `Content-Type: application/json`:
- **Todos los roles (`POSTULANT`, `RECRUITER`, `INSTITUTION`):**
  - `POST /api/agent/chat` *(asistente flotante / chat con memoria persistente)*
  - `POST /api/agent/suggest` *(sugerencias estructuradas de vacantes)*
  - `POST /api/agent/optimize-bio` *(botón "Optimizar con IA" en el editor de biografía/descripción)*
- **Solo `POSTULANT`:**
  - `POST /api/agent/postulant/talent-analysis` *(diagnóstico de brecha de habilidades en perfil/talento)*
- **Solo `RECRUITER`:**
  - `POST /api/agent/recruiter/design-job` *(asistente de creación de vacantes; devuelve `JobBlueprintSuggestionDTO` para autocompletar el formulario)*
- **Solo `INSTITUTION`:**
  - `POST /api/agent/institution/employability-insights` *(panel de diagnóstico ejecutivo en el dashboard institucional)*

#### B. Servidor MCP (`POST /mcp`) — Exclusivo para Clientes MCP Externos / Postman
El endpoint **`POST /mcp`** (`McpServerConfig` + `UnextMcpProvider`) implementa el protocolo **MCP JSON-RPC 2.0** en modo `STATELESS` (`spring.ai.mcp.server.protocol=STATELESS`) para clientes MCP externos (Claude Desktop, MCP Inspector, otros agentes externos o pruebas JSON-RPC en Postman). **No debe ser consumido por la interfaz Angular.**
- **Seguridad y Headers obligatorios en `/mcp`:**
  - `Authorization: Bearer <JWT>` *(protegido por `JwtAuthenticationFilter` y `SecurityConfig`)*
  - `Content-Type: application/json`
  - `Accept: application/json, text/event-stream` *(requerido por `WebMvcStatelessServerTransport` de Spring AI 2.0)*
- **Capacidades expuestas vía MCP:**
  - **Tools (`McpServerConfig` vía `MethodToolCallbackProvider`):** las 7 tools de `PostulantTools`, `RecruiterTools` e `InstitutionTools` (cada ejecución verifica el rol del JWT con `AgentRuleGuard.requireCurrentUserWithRole(...)`).
  - **Resources (`UnextMcpProvider` vía `@McpResource`):**
    - `unext://resources/active-jobs` *(resumen de hasta 20 vacantes activas en `unext_db`)*
    - `unext://resources/my-profile` *(contexto RAG completo del usuario autenticado)*
    - `unext://resources/platform-rules` *(reglas de negocio oficiales de UNEXT)*
  - **Prompts (`UnextMcpProvider` vía `@McpPrompt`):**
    - `unext-optimize-bio` (`bio` requerido)
    - `unext-design-job` (`prompt` requerido)
    - `unext-talent-analysis` (`targetRole` opcional)
    - `unext-employability-insights` (`career` opcional)
