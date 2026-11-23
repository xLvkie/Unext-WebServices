package com.nextworks.unextwebservices.agent.tools;

import com.nextworks.unextwebservices.entity.InternshipAgreement;
import com.nextworks.unextwebservices.entity.JobOffer;
import com.nextworks.unextwebservices.entity.RecruiterProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.repository.InternshipAgreementRepository;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.JobOfferRepository;
import com.nextworks.unextwebservices.repository.RecruiterProfileRepository;
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
public class RecruiterTools {

    private final AgentRuleGuard agentRuleGuard;
    private final RecruiterProfileRepository recruiterRepository;
    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository applicationRepository;
    private final InternshipAgreementRepository agreementRepository;
    private final MatchingService matchingService;

    @Transactional(readOnly = true)
    @Tool(
            name = "consultarMisVacantesEmpresa",
            description = "Consulta las vacantes activas publicadas por la empresa del reclutador autenticado en UNEXT, incluyendo modalidad, nivel, salario en S/., habilidades requeridas y total de postulantes."
    )
    public String consultarMisVacantesEmpresa() {
        User user = agentRuleGuard.requireCurrentUserWithRole(Role.RECRUITER);
        Optional<RecruiterProfile> profileOpt = recruiterRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return "El reclutador aún no ha completado el perfil de su empresa en UNEXT.";
        }

        RecruiterProfile profile = profileOpt.get();
        List<JobOffer> offers = jobOfferRepository.findByRecruiterProfileIdOrderByCreatedAtDesc(profile.getId());
        if (offers.isEmpty()) {
            return "La empresa " + profile.getCompanyName() + " aún no tiene vacantes publicadas en UNEXT.";
        }

        return offers.stream()
                .map(offer -> {
                    List<String> reqSkills = matchingService.requiredSkillNames(offer);
                    int applicantsCount = applicationRepository.findByJobOfferIdOrderByCreatedAtDesc(offer.getId()).size();
                    String state = Boolean.TRUE.equals(offer.getIsActive()) ? "ACTIVA" : "CERRADA";
                    String salary = formatSalaryRange(offer);
                    return String.format(
                            "- %s [%s] | Modalidad: %s | Nivel: %s | Salario: %s | Postulantes: %d | Skills: %s",
                            offer.getTitle(),
                            state,
                            offer.getModality(),
                            offer.getExperienceLevel(),
                            salary,
                            applicantsCount,
                            reqSkills.isEmpty() ? "No especificadas" : String.join(", ", reqSkills)
                    );
                })
                .collect(Collectors.joining("\n", "Vacantes de " + profile.getCompanyName() + " (" + offers.size() + "):\n", ""));
    }

    @Transactional(readOnly = true)
    @Tool(
            name = "consultarConveniosEmpresa",
            description = "Consulta los convenios de prácticas formativas registrados por la empresa del reclutador autenticado."
    )
    public String consultarConveniosEmpresa() {
        User user = agentRuleGuard.requireCurrentUserWithRole(Role.RECRUITER);
        Optional<RecruiterProfile> profileOpt = recruiterRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return "El reclutador aún no ha completado el perfil de su empresa en UNEXT.";
        }

        RecruiterProfile profile = profileOpt.get();
        List<InternshipAgreement> agreements = agreementRepository.findByRecruiterProfileIdOrderByCreatedAtDesc(profile.getId());
        if (agreements.isEmpty()) {
            return "La empresa " + profile.getCompanyName() + " no tiene convenios de prácticas registrados.";
        }

        return agreements.stream()
                .map(a -> String.format(
                        "- %s | Estado: %s | Estudiante: %s %s | Institución: %s | Periodo: %s a %s (%d h/sem)",
                        a.getTitle(),
                        a.getStatus(),
                        a.getPostulantProfile().getFirstName(),
                        a.getPostulantProfile().getLastName(),
                        a.getInstitutionProfile().getName(),
                        a.getStartDate(),
                        a.getEndDate(),
                        a.getWeeklyHours()
                ))
                .collect(Collectors.joining("\n", "Convenios de " + profile.getCompanyName() + " (" + agreements.size() + "):\n", ""));
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
