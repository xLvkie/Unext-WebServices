package com.nextworks.unextwebservices.service;

import com.nextworks.unextwebservices.entity.InstitutionProfile;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.repository.InstitutionEndorsementRepository;
import com.nextworks.unextwebservices.repository.InstitutionProfileRepository;
import com.nextworks.unextwebservices.repository.InternshipAgreementRepository;
import com.nextworks.unextwebservices.repository.JobApplicationRepository;
import com.nextworks.unextwebservices.repository.PostulantProfileRepository;
import com.nextworks.unextwebservices.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String EMAIL = "inst@test.com";

    @Mock private UserRepository userRepository;
    @Mock private InstitutionProfileRepository institutionRepository;
    @Mock private PostulantProfileRepository postulantRepository;
    @Mock private JobApplicationRepository applicationRepository;
    @Mock private InternshipAgreementRepository agreementRepository;
    @Mock private InstitutionEndorsementRepository endorsementRepository;
    @InjectMocks private DashboardService dashboardService;

    private final UUID instId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(UUID.randomUUID());
        InstitutionProfile institution = new InstitutionProfile();
        institution.setId(instId);
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        lenient().when(institutionRepository.findByUserId(user.getId())).thenReturn(Optional.of(institution));
    }

    @Test
    void summaryDevuelve503ConMensajeCuandoFallaLaBd() {
        when(postulantRepository.countVerifiedByInstitutionAndCareer(any(), anyString()))
                .thenThrow(new DataAccessResourceFailureException("conexion perdida"));

        assertThatThrownBy(() -> dashboardService.getSummary(EMAIL, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getReason()).isEqualTo("Error al cargar las métricas. Por favor, intente de nuevo en unos minutos");
                });
    }

    @Test
    void topCompaniesDevuelve503CuandoFallaLaBd() {
        when(applicationRepository.findTopCompanies(any(), anyString(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("conexion perdida"));

        assertThatThrownBy(() -> dashboardService.getTopCompanies(EMAIL, 5, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void demandByCareerDevuelve503CuandoFallaLaBd() {
        when(postulantRepository.findDemandByCareer(any(), any()))
                .thenThrow(new DataAccessResourceFailureException("conexion perdida"));

        assertThatThrownBy(() -> dashboardService.getDemandByCareer(EMAIL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void topCompaniesRechazaLimitFueraDeRangoSinConsultarLaBd() {
        for (int invalid : new int[]{0, -1, 51}) {
            assertThatThrownBy(() -> dashboardService.getTopCompanies(EMAIL, invalid, null))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getReason()).contains("entre 1 y 50");
                    });
        }
        verify(applicationRepository, never()).findTopCompanies(any(), any(), any(), any());
    }

    @Test
    void summarySinAlumnosVerificadosDevuelveTasaCeroSinDividirPorCero() {
        when(postulantRepository.countVerifiedByInstitutionAndCareer(instId, "")).thenReturn(0L);
        when(postulantRepository.countHiredByInstitutionAndCareer(eq(instId), eq(""), any())).thenReturn(0L);

        var result = dashboardService.getSummary(EMAIL, "  ");

        assertThat(result.getEmployabilityRate()).isEqualTo(0.0);
        assertThat(result.getMessage()).isNull();
        assertThat(result.getCareer()).isNull();
    }

    @Test
    void summaryConCarreraSinAlumnosNormalizaFiltroYAvisaSinDatos() {
        when(postulantRepository.countStudentsByInstitutionAndCareer(instId, "medicina")).thenReturn(0L);

        var result = dashboardService.getSummary(EMAIL, "  MeDiCiNa ");

        verify(postulantRepository).countVerifiedByInstitutionAndCareer(instId, "medicina");
        assertThat(result.getMessage()).isEqualTo("No hay datos suficientes para el filtro seleccionado");
        assertThat(result.getTotalVerifiedStudents()).isZero();
        assertThat(result.getCareer()).isEqualTo("MeDiCiNa");
    }
}
