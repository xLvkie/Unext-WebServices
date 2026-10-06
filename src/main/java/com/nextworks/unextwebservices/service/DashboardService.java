package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.dto.dashboard.CareerDemandDTO;
import com.nextworks.unextwebservices.dto.dashboard.DashboardSummaryResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.DemandByCareerResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.TopCompaniesResponseDTO;
import com.nextworks.unextwebservices.dto.dashboard.TopCompanyDTO;
import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import com.nextworks.unextwebservices.entity.enums.ApplicationStatus;
import com.nextworks.unextwebservices.repository.InstitutionEndorsementRepository;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.InternshipAgreementRepository;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    static final String NO_DATA_MESSAGE = "No hay datos suficientes para el filtro seleccionado";
    static final String DB_ERROR_MESSAGE = "Error al cargar las métricas. Por favor, intente de nuevo en unos minutos";
    static final int MIN_LIMIT = 1;
    static final int MAX_LIMIT = 50;

    private final UserRepository userRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final PostulantProfileRepository postulantRepository;
    private final JobApplicationRepository applicationRepository;
    private final InternshipAgreementRepository agreementRepository;
    private final InstitutionEndorsementRepository endorsementRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponseDTO getSummary(String email, String careerFilter) {
        String career = normalizeCareer(careerFilter);
        try {
            UUID instId = findInstitution(email).getId();

            long verified = postulantRepository.countVerifiedByInstitutionAndCareer(instId, career);
            long hired = postulantRepository.countHiredByInstitutionAndCareer(instId, career, ApplicationStatus.ACCEPTED);
            long activeAgreements = agreementRepository.countByInstitutionAndStatusAndCareer(instId, AgreementStatus.APPROVED, career);
            long endorsed = endorsementRepository.countByInstitutionProfileIdAndRevokedAtIsNull(instId);
            double rate = verified > 0 ? Math.round(((double) hired / verified) * 10000.0) / 100.0 : 0.0;

            return DashboardSummaryResponseDTO.builder()
                    .career(displayCareer(careerFilter))
                    .totalVerifiedStudents(verified)
                    .totalHiredStudents(hired)
                    .totalActiveAgreements(activeAgreements)
                    .totalEndorsedCompanies(endorsed)
                    .employabilityRate(rate)
                    .message(noDataMessage(instId, career))
                    .build();
        } catch (DataAccessException e) {
            throw dbError();
        }
    }

    @Transactional(readOnly = true)
    public TopCompaniesResponseDTO getTopCompanies(String email, int limit, String careerFilter) {
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El parámetro limit debe estar entre " + MIN_LIMIT + " y " + MAX_LIMIT);
        }
        String career = normalizeCareer(careerFilter);
        try {
            UUID instId = findInstitution(email).getId();
            List<TopCompanyDTO> companies = applicationRepository.findTopCompanies(
                    instId, career, ApplicationStatus.ACCEPTED, PageRequest.of(0, limit));

            return TopCompaniesResponseDTO.builder()
                    .career(displayCareer(careerFilter))
                    .companies(companies)
                    .message(noDataMessage(instId, career))
                    .build();
        } catch (DataAccessException e) {
            throw dbError();
        }
    }

    @Transactional(readOnly = true)
    public DemandByCareerResponseDTO getDemandByCareer(String email) {
        try {
            UUID instId = findInstitution(email).getId();
            List<CareerDemandDTO> careers = postulantRepository.findDemandByCareer(instId, ApplicationStatus.ACCEPTED);

            return DemandByCareerResponseDTO.builder()
                    .careers(careers)
                    .build();
        } catch (DataAccessException e) {
            throw dbError();
        }
    }

    private InstitutionProfile findInstitution(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return institutionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de institución no encontrado"));
    }

    // Con filtro y sin alumnos de esa carrera en la institucion: mensaje de "sin datos"
    private String noDataMessage(UUID instId, String career) {
        if (!career.isEmpty() && postulantRepository.countStudentsByInstitutionAndCareer(instId, career) == 0) {
            return NO_DATA_MESSAGE;
        }
        return null;
    }

    // trim + minusculas; vacio = sin filtro
    private String normalizeCareer(String career) {
        return career == null ? "" : career.trim().toLowerCase(Locale.ROOT);
    }

    private String displayCareer(String career) {
        return (career == null || career.isBlank()) ? null : career.trim();
    }

    private ResponseStatusException dbError() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, DB_ERROR_MESSAGE);
    }
}
