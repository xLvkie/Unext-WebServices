package com.nextworks.unextwebservices.agent.tools;

import com.nextworks.unextwebservices.entity.JobApplication;
import com.nextworks.unextwebservices.entity.JobOffer;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.StudentSkill;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.JobOfferRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.StudentSkillRepository;
import com.nextworks.unextwebservices.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostulantTools {

    private final AgentRuleGuard agentRuleGuard;
    private final PostulantProfileRepository postulantRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository applicationRepository;
    private final MatchingService matchingService;

    @Transactional(readOnly = true)
    @Tool(
            name = "obtenerMisHabilidadesPostulante",
            description = "Obtiene el perfil académico y las habilidades técnicas registradas y validadas del postulante autenticado en UNEXT."
    )
    public String obtenerMisHabilidadesPostulante() {
        User user = agentRuleGuard.requireCurrentUserWithRole(Role.POSTULANT);
        Optional<PostulantProfile> profileOpt = postulantRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return "El postulante aún no ha completado su perfil en UNEXT.";
        }

        PostulantProfile profile = profileOpt.get();
        List<StudentSkill> skills = studentSkillRepository.findByPostulantProfileId(profile.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Perfil del Postulante: ")
                .append(profile.getFirstName()).append(" ").append(profile.getLastName()).append("\n");
        sb.append("Carrera: ").append(profile.getCareer() != null ? profile.getCareer() : "No especificada").append("\n");
        if (profile.getCurrentCycle() != null) {
            sb.append("Ciclo actual: ").append(profile.getCurrentCycle()).append("\n");
        }
        sb.append("Verificación institucional: ")
                .append(Boolean.TRUE.equals(profile.getIsInstitutionVerified()) ? "Verificado" : "Pendiente");
        if (profile.getInstitutionProfile() != null) {
            sb.append(" (").append(profile.getInstitutionProfile().getName()).append(")");
        }
        sb.append("\n");

        if (skills.isEmpty()) {
            sb.append("Habilidades registradas: Ninguna habilidad registrada aún.");
        } else {
            sb.append("Habilidades registradas (").append(skills.size()).append("):\n");
            for (StudentSkill skill : skills) {
                sb.append("- ").append(skill.getName())
                        .append(" | Dominio: ").append(skill.getMasteryLevel());
                if (Boolean.TRUE.equals(skill.getIsTopSkill())) {
                    sb.append(" | Destacada");
                }
                if (Boolean.TRUE.equals(skill.getIsValidatedByInstitution())) {
                    sb.append(" | Certificada por ").append(
                            skill.getValidatedInstitutionName() != null
                                    ? skill.getValidatedInstitutionName()
                                    : "su institución");
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    @Transactional(readOnly = true)
    @Tool(
            name = "listarVacantesActivasDisponibles",
            description = "Lista las vacantes de empleo y prácticas activas disponibles en UNEXT con título, empresa, modalidad, nivel de experiencia, salario en S/. y habilidades requeridas."
    )
    public String listarVacantesActivasDisponibles() {
        agentRuleGuard.requireCurrentUserWithRole(Role.POSTULANT);
        List<JobOffer> offers = jobOfferRepository.findByIsActiveTrue();
        if (offers.isEmpty()) {
            return "Actualmente no hay vacantes activas registradas en UNEXT.";
        }

        return offers.stream()
                .limit(15)
                .map(offer -> {
                    List<String> reqSkills = matchingService.requiredSkillNames(offer);
                    String salary = formatSalaryRange(offer);
                    return String.format(
                            "- %s | Empresa: %s | Modalidad: %s | Nivel: %s | Ubicación: %s | Salario: %s | Skills: %s",
                            offer.getTitle(),
                            offer.getRecruiterProfile().getCompanyName(),
                            offer.getModality(),
                            offer.getExperienceLevel(),
                            offer.getLocation() != null ? offer.getLocation() : "No especificada",
                            salary,
                            reqSkills.isEmpty() ? "No especificadas" : String.join(", ", reqSkills)
                    );
                })
                .collect(Collectors.joining("\n", "Vacantes activas disponibles en UNEXT (" + offers.size() + " total):\n", ""));
    }

    @Transactional(readOnly = true)
    @Tool(
            name = "consultarMisPostulaciones",
            description = "Consulta el listado y estado actual de las postulaciones del postulante autenticado en UNEXT."
    )
    public String consultarMisPostulaciones() {
        User user = agentRuleGuard.requireCurrentUserWithRole(Role.POSTULANT);
        Optional<PostulantProfile> profileOpt = postulantRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return "El postulante aún no ha completado su perfil en UNEXT.";
        }

        List<JobApplication> apps = applicationRepository.findByPostulantProfileId(profileOpt.get().getId());
        if (apps.isEmpty()) {
            return "Aún no tienes postulaciones registradas en UNEXT.";
        }

        return apps.stream()
                .map(app -> String.format(
                        "- Vacante: %s | Empresa: %s | Estado: %s | Fecha: %s",
                        app.getJobOffer().getTitle(),
                        app.getJobOffer().getRecruiterProfile().getCompanyName(),
                        app.getStatus(),
                        app.getCreatedAt()
                ))
                .collect(Collectors.joining("\n", "Tus postulaciones (" + apps.size() + "):\n", ""));
    }

    private String formatSalaryRange(JobOffer offer) {
        if (offer.getMinSalary() != null && offer.getMaxSalary() != null) {
            return "S/. " + offer.getMinSalary() + " - S/. " + offer.getMaxSalary();
        }
        if (offer.getMinSalary() != null) {
            return "Desde S/. " + offer.getMinSalary();
        }
        if (offer.getMaxSalary() != null) {
            return "Hasta S/. " + offer.getMaxSalary();
        }
        return "A tratar";
    }
}
