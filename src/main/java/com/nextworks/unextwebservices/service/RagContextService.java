package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.dashboard.CareerDemandDTO;
import com.nextworks.unextwebservices.dto.dashboard.DashboardSummaryResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.TopCompanyDTO;
import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.JobOffer;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.RecruiterProfile;
import com.nextworks.unextwebservices.entity.StudentSkill;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.JobOfferRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.RecruiterProfileRepository;
import com.nextworks.unextwebservices.repository.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagContextService {

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "con", "por", "una", "uno", "unos", "unas", "los", "las",
            "del", "que", "como", "sobre", "entre", "desde", "hasta", "esta",
            "este", "esto", "estos", "estas", "pero", "donde", "cual", "quien",
            "mis", "tus", "sus", "nos", "les", "muy", "mas", "sin", "hay",
            "tengo", "puedo", "quiero", "favor", "hola", "ayuda", "unext"
    );

    private final PostulantProfileRepository postulantRepository;
    private final RecruiterProfileRepository recruiterRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final JobOfferRepository jobOfferRepository;
    private final DashboardService dashboardService;
    private final MatchingService matchingService;

    public String getPlatformRulesContext() {
        return """
                [REGLAS OFICIALES DE UNEXT]
                - Segmentos de la plataforma: POSTULANT (estudiantes y egresados), RECRUITER (empresas) e INSTITUTION (universidades/institutos).
                - Mercado laboral peruano: todos los salarios estimados o publicados se expresan en Soles peruanos (S/.).
                - Umbral de compatibilidad recomendada: >= 80% de coincidencia entre StudentSkill y JobRequiredSkill (etiqueta ALTA).
                - Convenios de prácticas: solo aplican a vacantes de nivel TRAINEE con estudiantes verificados académicamente por su institución.
                - Acreditación institucional: las universidades pueden otorgar insignias de empresa aliada tras aprobar un convenio y certificar habilidades técnicas del postulante.
                - Regla anti-alucinación: responde únicamente con base en los datos reales recuperados de PostgreSQL (unext_db). Si no existen datos registrados, indícalo con claridad sin inventar vacantes, empresas ni métricas.""";
    }

    @Transactional(readOnly = true)
    public String getActiveJobsSummary(int limit) {
        List<JobOffer> offers = jobOfferRepository.findByIsActiveTrue();
        if (offers.isEmpty()) {
            return "No hay vacantes activas registradas actualmente en unext_db.";
        }

        return offers.stream()
                .sorted(Comparator.comparing(JobOffer::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::formatOfferSummary)
                .collect(Collectors.joining("\n", "Vacantes activas en UNEXT (" + offers.size() + " disponibles):\n", ""));
    }

    @Transactional(readOnly = true)
    public String retrieveContext(User user, String query) {
        StringBuilder context = new StringBuilder();
        context.append("=== CONTEXTO RAG RECUPERADO DE UNEXT_DB ===\n");
        context.append(getPlatformRulesContext()).append("\n\n");

        if (user != null) {
            appendUserProfileContext(user, context);
            context.append("\n");
        }

        appendMarketVacanciesContext(query, context);
        context.append("=== FIN DEL CONTEXTO RAG ===");
        return context.toString();
    }

    private void appendUserProfileContext(User user, StringBuilder context) {
        context.append("[PERFIL REAL DEL USUARIO AUTENTICADO (").append(user.getRole().name()).append(")]\n");
        switch (user.getRole()) {
            case POSTULANT -> {
                Optional<PostulantProfile> profileOpt = postulantRepository.findByUserId(user.getId());
                if (profileOpt.isEmpty()) {
                    context.append("- Perfil de postulante aún incompleto en base de datos.\n");
                    return;
                }
                PostulantProfile p = profileOpt.get();
                List<StudentSkill> skills = studentSkillRepository.findByPostulantProfileId(p.getId());
                context.append("- Nombre: ").append(p.getFirstName()).append(" ").append(p.getLastName()).append("\n");
                context.append("- Carrera: ").append(nullSafe(p.getCareer(), "No especificada")).append("\n");
                if (p.getCurrentCycle() != null) {
                    context.append("- Ciclo: ").append(p.getCurrentCycle()).append("\n");
                }
                context.append("- Estado académico: ").append(nullSafe(p.getAcademicStatus(), "No especificado")).append("\n");
                context.append("- Verificado por institución: ")
                        .append(Boolean.TRUE.equals(p.getIsInstitutionVerified()) ? "Sí" : "No");
                if (p.getInstitutionProfile() != null) {
                    context.append(" (").append(p.getInstitutionProfile().getName()).append(")");
                }
                context.append("\n");
                if (p.getBio() != null && !p.getBio().isBlank()) {
                    context.append("- Biografía actual: ").append(p.getBio()).append("\n");
                }
                if (skills.isEmpty()) {
                    context.append("- Habilidades registradas: Ninguna.\n");
                } else {
                    String skillList = skills.stream()
                            .map(s -> s.getName() + " (" + s.getMasteryLevel()
                                    + (Boolean.TRUE.equals(s.getIsValidatedByInstitution()) ? ", Validada" : "") + ")")
                            .collect(Collectors.joining(", "));
                    context.append("- Habilidades registradas: ").append(skillList).append("\n");
                }
            }
            case RECRUITER -> {
                Optional<RecruiterProfile> profileOpt = recruiterRepository.findByUserId(user.getId());
                if (profileOpt.isEmpty()) {
                    context.append("- Perfil de empresa aún incompleto en base de datos.\n");
                    return;
                }
                RecruiterProfile r = profileOpt.get();
                List<JobOffer> myOffers = jobOfferRepository.findByRecruiterProfileIdOrderByCreatedAtDesc(r.getId());
                long activeOffers = myOffers.stream().filter(o -> Boolean.TRUE.equals(o.getIsActive())).count();
                context.append("- Empresa: ").append(r.getCompanyName()).append(" (RUC: ").append(r.getRuc()).append(")\n");
                context.append("- Industria: ").append(nullSafe(r.getIndustry(), "No especificada")).append("\n");
                context.append("- Cuenta verificada: ").append(Boolean.TRUE.equals(r.getIsVerified()) ? "Sí" : "No")
                        .append(" | Acreditada por universidades: ")
                        .append(Boolean.TRUE.equals(r.getIsValidated()) ? "Sí" : "No").append("\n");
                context.append("- Vacantes publicadas: ").append(myOffers.size())
                        .append(" (Activas: ").append(activeOffers).append(")\n");
            }
            case INSTITUTION -> {
                Optional<InstitutionProfile> profileOpt = institutionRepository.findByUserId(user.getId());
                if (profileOpt.isEmpty()) {
                    context.append("- Perfil institucional aún incompleto en base de datos.\n");
                    return;
                }
                InstitutionProfile inst = profileOpt.get();
                DashboardSummaryResponseDTO summary = dashboardService.getSummary(user.getEmail(), null);
                List<CareerDemandDTO> careers = dashboardService.getDemandByCareer(user.getEmail()).getCareers();
                context.append("- Institución: ").append(inst.getName())
                        .append(" (Dominio: ").append(inst.getDomain()).append(")\n");
                context.append("- Alumnos verificados: ").append(summary.getTotalVerifiedStudents())
                        .append(" | Contratados: ").append(summary.getTotalHiredStudents())
                        .append(" | Tasa empleabilidad: ").append(summary.getEmployabilityRate()).append("%\n");
                context.append("- Convenios activos: ").append(summary.getTotalActiveAgreements())
                        .append(" | Empresas aliadas: ").append(summary.getTotalEndorsedCompanies()).append("\n");
                if (careers != null && !careers.isEmpty()) {
                    String careerText = careers.stream()
                            .limit(5)
                            .map(c -> c.getCareer() + " (" + c.getApplications() + " post., " + c.getAccepted() + " acept.)")
                            .collect(Collectors.joining("; "));
                    context.append("- Demanda por carrera: ").append(careerText).append("\n");
                }
            }
        }
    }

    private void appendMarketVacanciesContext(String query, StringBuilder context) {
        List<JobOffer> activeOffers = jobOfferRepository.findByIsActiveTrue();
        context.append("[VACANTES ACTIVAS Y DEMANDA REAL EN UNEXT_DB]\n");
        if (activeOffers.isEmpty()) {
            context.append("- Actualmente no existen vacantes activas registradas en unext_db.\n");
            return;
        }

        Set<String> keywords = extractKeywords(query);
        List<JobOffer> selected = activeOffers.stream()
                .map(offer -> new ScoredOffer(offer, scoreOffer(offer, keywords)))
                .sorted(Comparator.comparingInt(ScoredOffer::score).reversed()
                        .thenComparing(s -> s.offer().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(ScoredOffer::offer)
                .toList();

        context.append("- Total de vacantes activas en plataforma: ").append(activeOffers.size()).append("\n");
        for (JobOffer offer : selected) {
            context.append(formatOfferSummary(offer)).append("\n");
        }

        Map<String, Long> topSkills = new LinkedHashMap<>();
        activeOffers.stream()
                .flatMap(offer -> matchingService.requiredSkillNames(offer).stream())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .forEach(e -> topSkills.put(e.getKey(), e.getValue()));

        if (!topSkills.isEmpty()) {
            String demanded = topSkills.entrySet().stream()
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .collect(Collectors.joining(", "));
            context.append("- Habilidades más demandadas en vacantes activas: ").append(demanded).append("\n");
        }
    }

    private Set<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}+#.]+"))
                .map(String::trim)
                .filter(t -> t.length() >= 3 && !STOP_WORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private int scoreOffer(JobOffer offer, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        String searchable = (nullSafe(offer.getTitle(), "") + " "
                + nullSafe(offer.getDescription(), "") + " "
                + nullSafe(offer.getRequirements(), "") + " "
                + offer.getModality().name() + " "
                + offer.getExperienceLevel().name() + " "
                + offer.getRecruiterProfile().getCompanyName() + " "
                + String.join(" ", matchingService.requiredSkillNames(offer)))
                .toLowerCase(Locale.ROOT);

        int score = 0;
        for (String kw : keywords) {
            if (searchable.contains(kw)) {
                score++;
            }
        }
        return score;
    }

    private String formatOfferSummary(JobOffer offer) {
        List<String> skills = matchingService.requiredSkillNames(offer);
        String salary;
        if (offer.getMinSalary() != null && offer.getMaxSalary() != null) {
            salary = "S/. " + offer.getMinSalary() + " - S/. " + offer.getMaxSalary();
        } else if (offer.getMinSalary() != null) {
            salary = "Desde S/. " + offer.getMinSalary();
        } else if (offer.getMaxSalary() != null) {
            salary = "Hasta S/. " + offer.getMaxSalary();
        } else {
            salary = "A tratar";
        }
        return String.format(
                "  * %s | Empresa: %s | Modalidad: %s | Nivel: %s | Salario: %s | Skills: %s",
                offer.getTitle(),
                offer.getRecruiterProfile().getCompanyName(),
                offer.getModality(),
                offer.getExperienceLevel(),
                salary,
                skills.isEmpty() ? "No especificadas" : String.join(", ", skills)
        );
    }

    private String nullSafe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    @Transactional(readOnly = true)
    public TalentSnapshot buildTalentSnapshot(User user, String targetRole) {
        PostulantProfile profile = postulantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Primero debes completar tu perfil de postulante para analizar tu brecha de habilidades."));

        List<StudentSkill> studentSkills = studentSkillRepository.findByPostulantProfileId(profile.getId());
        List<String> currentSkills = studentSkills.stream()
                .map(StudentSkill::getName)
                .toList();

        Set<String> normalizedOwned = studentSkills.stream()
                .map(s -> s.getName().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<JobOffer> allActive = jobOfferRepository.findByIsActiveTrue();
        Set<String> targetKeywords = extractKeywords(targetRole);
        List<JobOffer> relevantOffers = allActive;
        if (!targetKeywords.isEmpty()) {
            List<JobOffer> filtered = allActive.stream()
                    .filter(o -> scoreOffer(o, targetKeywords) > 0)
                    .toList();
            if (!filtered.isEmpty()) {
                relevantOffers = filtered;
            }
        }

        Map<String, String> canonicalNames = new LinkedHashMap<>();
        Map<String, Long> skillDemandCounts = new LinkedHashMap<>();
        for (JobOffer offer : relevantOffers) {
            for (String rawSkill : matchingService.requiredSkillNames(offer)) {
                String trimmed = rawSkill.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String key = trimmed.toLowerCase(Locale.ROOT);
                canonicalNames.putIfAbsent(key, trimmed);
                skillDemandCounts.merge(key, 1L, Long::sum);
            }
        }

        List<String> demandedOrdered = skillDemandCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        List<String> matchedDemandSkills = new ArrayList<>();
        List<String> missingKeySkills = new ArrayList<>();
        for (String key : demandedOrdered) {
            String display = canonicalNames.get(key);
            if (normalizedOwned.contains(key)) {
                matchedDemandSkills.add(display);
            } else if (missingKeySkills.size() < 10) {
                missingKeySkills.add(display);
            }
        }

        String fullName = (profile.getFirstName() + " " + profile.getLastName()).trim();
        String career = nullSafe(profile.getCareer(), "Carrera no especificada");
        String marketDetail = relevantOffers.stream()
                .limit(6)
                .map(this::formatOfferSummary)
                .collect(Collectors.joining("\n"));

        return new TalentSnapshot(
                fullName,
                career,
                currentSkills,
                matchedDemandSkills,
                missingKeySkills,
                relevantOffers.size(),
                marketDetail
        );
    }

    @Transactional(readOnly = true)
    public InstitutionSnapshot buildInstitutionSnapshot(User user, String careerFilter) {
        InstitutionProfile institution = institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Primero debes completar tu perfil institucional para generar el diagnóstico."));

        DashboardSummaryResponseDTO summary = dashboardService.getSummary(user.getEmail(), careerFilter);
        List<CareerDemandDTO> careers = dashboardService.getDemandByCareer(user.getEmail()).getCareers();
        List<TopCompanyDTO> companies = dashboardService.getTopCompanies(user.getEmail(), 5, careerFilter).getCompanies();

        String careerSummary = (careers == null || careers.isEmpty())
                ? "Sin datos de demanda por carrera."
                : careers.stream()
                        .map(c -> c.getCareer() + ": " + c.getApplications() + " postulaciones, " + c.getAccepted() + " aceptaciones")
                        .collect(Collectors.joining("; "));

        String companiesSummary = (companies == null || companies.isEmpty())
                ? "Sin empresas contratantes registradas aún."
                : companies.stream()
                        .map(c -> c.getCompanyName() + " (" + c.getHiredStudents() + " contratados)")
                        .collect(Collectors.joining("; "));

        return new InstitutionSnapshot(
                institution.getName(),
                summary.getCareer(),
                summary.getTotalVerifiedStudents(),
                summary.getTotalHiredStudents(),
                summary.getTotalActiveAgreements(),
                summary.getTotalEndorsedCompanies(),
                summary.getEmployabilityRate(),
                careerSummary,
                companiesSummary,
                summary.getMessage()
        );
    }

    public record TalentSnapshot(
            String postulantName,
            String career,
            List<String> currentSkills,
            List<String> matchedDemandSkills,
            List<String> missingKeySkills,
            int activeVacanciesAnalyzed,
            String marketDetail
    ) {}

    public record InstitutionSnapshot(
            String institutionName,
            String careerFilter,
            long totalVerifiedStudents,
            long totalHiredStudents,
            long totalActiveAgreements,
            long totalEndorsedCompanies,
            double employabilityRate,
            String careerSummary,
            String companiesSummary,
            String noDataMessage
    ) {}

    private record ScoredOffer(JobOffer offer, int score) {}
}
