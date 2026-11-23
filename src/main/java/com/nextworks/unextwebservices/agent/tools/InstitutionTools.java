package com.nextworks.unextwebservices.agent.tools;

import com.nextworks.unextwebservices.dto.dashboard.CareerDemandDTO;
import com.nextworks.unextwebservices.dto.dashboard.DashboardSummaryResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.TopCompanyDTO;
import com.nextworks.unextwebservices.dto.validation.PendingStudentResponseDTO;
import com.nextworks.unextwebservices.dto.validation.PendingValidationResponseDTO;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.Role;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.service.DashboardService;
import com.nextworks.unextwebservices.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InstitutionTools {

    private final AgentRuleGuard agentRuleGuard;
    private final DashboardService dashboardService;
    private final InstitutionService institutionService;
    private final InstitutionProfileRepository institutionRepository;

    @Transactional(readOnly = true)
    @Tool(
            name = "consultarEstadisticasEmpleabilidadInstitucion",
            description = "Consulta las estadísticas reales de empleabilidad, alumnos verificados, alumnos contratados, convenios activos, empresas aliadas y demanda por carrera de la institución autenticada."
    )
    public String consultarEstadisticasEmpleabilidadInstitucion() {
        User user = agentRuleGuard.requireCurrentUserWithRole(Role.INSTITUTION);
        if (institutionRepository.findByUserId(user.getId()).isEmpty()) {
            return "La institución aún no ha completado su perfil institucional en UNEXT.";
        }

        DashboardSummaryResponseDTO summary = dashboardService.getSummary(user.getEmail(), null);
        List<CareerDemandDTO> careers = dashboardService.getDemandByCareer(user.getEmail()).getCareers();
        List<TopCompanyDTO> topCompanies = dashboardService.getTopCompanies(user.getEmail(), 5, null).getCompanies();

        StringBuilder sb = new StringBuilder();
        sb.append("Métricas Institucionales de Empleabilidad:\n");
        sb.append("- Alumnos verificados: ").append(summary.getTotalVerifiedStudents()).append("\n");
        sb.append("- Alumnos contratados: ").append(summary.getTotalHiredStudents()).append("\n");
        sb.append("- Tasa de empleabilidad: ").append(summary.getEmployabilityRate()).append("%\n");
        sb.append("- Convenios de prácticas aprobados: ").append(summary.getTotalActiveAgreements()).append("\n");
        sb.append("- Empresas aliadas acreditadas: ").append(summary.getTotalEndorsedCompanies()).append("\n");

        if (careers != null && !careers.isEmpty()) {
            sb.append("Demanda por carrera:\n");
            for (CareerDemandDTO c : careers) {
                sb.append("  * ").append(c.getCareer())
                        .append(": ").append(c.getApplications()).append(" postulaciones, ")
                        .append(c.getAccepted()).append(" aceptaciones\n");
            }
        }

        if (topCompanies != null && !topCompanies.isEmpty()) {
            sb.append("Top empresas contratantes:\n");
            for (TopCompanyDTO company : topCompanies) {
                sb.append("  * ").append(company.getCompanyName())
                        .append(" (").append(company.getHiredStudents()).append(" alumnos contratados)\n");
            }
        }
        return sb.toString().trim();
    }

    @Transactional(readOnly = true)
    @Tool(
            name = "consultarPendientesInstitucion",
            description = "Consulta los alumnos pendientes de verificación académica y las validaciones de habilidades técnicas pendientes de la institución autenticada."
    )
    public String consultarPendientesInstitucion() {
        User user = agentRuleGuard.requireCurrentUserWithRole(Role.INSTITUTION);
        if (institutionRepository.findByUserId(user.getId()).isEmpty()) {
            return "La institución aún no ha completado su perfil institucional en UNEXT.";
        }

        List<PendingStudentResponseDTO> pendingStudents = institutionService.getPendingStudents(user.getEmail());
        List<PendingValidationResponseDTO> pendingValidations = institutionService.getPendingSkillValidations(user.getEmail());

        StringBuilder sb = new StringBuilder();
        sb.append("Pendientes de Acreditación Institucional:\n");
        sb.append("- Estudiantes pendientes de verificación académica: ").append(pendingStudents.size()).append("\n");
        for (PendingStudentResponseDTO s : pendingStudents) {
            sb.append("  * ").append(s.getFirstName()).append(" ").append(s.getLastName())
                    .append(" (").append(s.getEmail()).append(")\n");
        }

        sb.append("- Habilidades técnicas pendientes de validación: ").append(pendingValidations.size()).append("\n");
        for (PendingValidationResponseDTO v : pendingValidations) {
            sb.append("  * ").append(v.getStudentName()).append(" -> ").append(v.getSkillName())
                    .append(" [Estado: ").append(v.getStatus()).append("]\n");
        }
        return sb.toString().trim();
    }
}
