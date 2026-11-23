package com.nextworks.unextwebservices.agent;

import com.nextworks.unextwebservices.agent.tools.AgentRuleGuard;
import com.nextworks.unextwebservices.agent.tools.InstitutionTools;
import com.nextworks.unextwebservices.agent.tools.PostulantTools;
import com.nextworks.unextwebservices.agent.tools.RecruiterTools;
import com.nextworks.unextwebservices.entity.JobOffer;
import com.nextworks.unextwebservices.entity.JobRequiredSkill;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.RecruiterProfile;
import com.nextworks.unextwebservices.entity.StudentSkill;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.InternshipAgreementRepository;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.JobOfferRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.RecruiterProfileRepository;
import com.nextworks.unextwebservices.repository.StudentSkillRepository;
import com.nextworks.unextwebservices.service.DashboardService;
import com.nextworks.unextwebservices.service.InstitutionService;
import com.nextworks.unextwebservices.service.MatchingService;
import com.nextworks.unextwebservices.service.RagContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentSecurityAndRagTest {

    private final AgentRuleGuard guard = new AgentRuleGuard();

    @Mock private PostulantProfileRepository postulantRepository;
    @Mock private RecruiterProfileRepository recruiterRepository;
    @Mock private InstitutionProfileRepository institutionRepository;
    @Mock private StudentSkillRepository studentSkillRepository;
    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private JobApplicationRepository applicationRepository;
    @Mock private InternshipAgreementRepository agreementRepository;
    @Mock private DashboardService dashboardService;
    @Mock private InstitutionService institutionService;
    @Mock private MatchingService matchingService;

    @Test
    void guardRejectsMissingAuthWith401AndWrongRoleWith403() {
        assertThatThrownBy(() -> guard.extractAuthenticatedUser(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        User postulant = User.builder().id(UUID.randomUUID()).email("alum@unext.pe").role(Role.POSTULANT).build();
        var auth = new UsernamePasswordAuthenticationToken(postulant, null, postulant.getAuthorities());

        assertThatThrownBy(() -> guard.requireRole(auth, Role.RECRUITER))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(guard.requireRole(auth, Role.POSTULANT)).isSameAs(postulant);
    }

    @Test
    void toolsEnforceStrictRoleIsolation() {
        User postulant = User.builder().id(UUID.randomUUID()).email("alum@unext.pe").role(Role.POSTULANT).build();
        PostulantTools postulantTools = new PostulantTools(
                guard, postulantRepository, studentSkillRepository, jobOfferRepository, applicationRepository, matchingService);
        RecruiterTools recruiterTools = new RecruiterTools(
                guard, recruiterRepository, jobOfferRepository, applicationRepository, agreementRepository, matchingService);
        InstitutionTools institutionTools = new InstitutionTools(
                guard, dashboardService, institutionService, institutionRepository);

        guard.executeWithUser(postulant, () -> {
            assertThatThrownBy(recruiterTools::consultarMisVacantesEmpresa)
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
            assertThatThrownBy(institutionTools::consultarEstadisticasEmpleabilidadInstitucion)
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

            when(postulantRepository.findByUserId(postulant.getId())).thenReturn(Optional.empty());
            assertThat(postulantTools.obtenerMisHabilidadesPostulante()).contains("no ha completado");
            return null;
        });

        assertThat(guard.resolveCurrentUserOptional()).isNull();
    }

    @Test
    void ragContextServiceRetrievesGroundedProfileAndActiveOffers() {
        User postulantUser = User.builder().id(UUID.randomUUID()).email("ana@uni.edu.pe").role(Role.POSTULANT).build();
        PostulantProfile profile = PostulantProfile.builder()
                .id(UUID.randomUUID())
                .user(postulantUser)
                .firstName("Ana")
                .lastName("Quispe")
                .career("Ingeniería de Software")
                .currentCycle(8)
                .isInstitutionVerified(true)
                .build();
        StudentSkill javaSkill = StudentSkill.builder()
                .id(UUID.randomUUID())
                .postulantProfile(profile)
                .name("Java")
                .masteryLevel("AVANZADO")
                .isValidatedByInstitution(true)
                .build();
        RecruiterProfile company = RecruiterProfile.builder()
                .id(UUID.randomUUID())
                .companyName("NextWorks Peru")
                .build();
        JobOffer offer = JobOffer.builder()
                .id(UUID.randomUUID())
                .recruiterProfile(company)
                .title("Practicante Backend Java")
                .description("Desarrollo de microservicios Spring Boot")
                .requirements("Java 21 y SQL")
                .modality(JobModality.HYBRID)
                .experienceLevel(ExperienceLevel.TRAINEE)
                .minSalary(new BigDecimal("1200"))
                .maxSalary(new BigDecimal("1600"))
                .isActive(true)
                .requiredSkills(List.of(JobRequiredSkill.builder().name("Java").build()))
                .build();

        when(postulantRepository.findByUserId(postulantUser.getId())).thenReturn(Optional.of(profile));
        when(studentSkillRepository.findByPostulantProfileId(profile.getId())).thenReturn(List.of(javaSkill));
        when(jobOfferRepository.findByIsActiveTrue()).thenReturn(List.of(offer));
        when(matchingService.requiredSkillNames(offer)).thenReturn(List.of("Java", "Spring Boot"));

        RagContextService ragService = new RagContextService(
                postulantRepository, recruiterRepository, institutionRepository,
                studentSkillRepository, jobOfferRepository, dashboardService, matchingService);

        String context = ragService.retrieveContext(postulantUser, "vacantes java backend");
        assertThat(context)
                .contains("Ana Quispe")
                .contains("Ingeniería de Software")
                .contains("Java (AVANZADO, Validada)")
                .contains("Practicante Backend Java")
                .contains("NextWorks Peru")
                .contains("S/. 1200 - S/. 1600");
    }
}
