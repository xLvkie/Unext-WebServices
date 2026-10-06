package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import com.nextworks.unextwebservices.entity.InternshipAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InternshipAgreementRepository extends JpaRepository<InternshipAgreement, UUID> {
    // Listar todos los convenios de la institución
    List<InternshipAgreement> findByInstitutionProfileIdOrderByCreatedAtDesc(UUID institutionId);
    // Listar convenios filtrados por su estado
    List<InternshipAgreement> findByInstitutionProfileIdAndStatusOrderByCreatedAtDesc(UUID institutionId, AgreementStatus status);
    long countByInstitutionProfileIdAndStatus(UUID institutionId, AgreementStatus status);

    // Dashboard: convenios por estado, opcionalmente filtrados por la carrera del alumno
    @Query("SELECT COUNT(a) FROM InternshipAgreement a WHERE a.institutionProfile.id = :institutionId " +
            "AND a.status = :status AND (:career = '' OR LOWER(TRIM(a.postulantProfile.career)) = :career)")
    long countByInstitutionAndStatusAndCareer(@Param("institutionId") UUID institutionId,
                                              @Param("status") AgreementStatus status, @Param("career") String career);

    // Para acreditar: solo se puede si hay al menos un convenio aprobado entre esa institucion y esa empresa
    boolean existsByInstitutionProfileIdAndRecruiterProfileIdAndStatus(UUID institutionProfileId, UUID recruiterProfileId, AgreementStatus status);
    // Convenio de otra institución: no se expone (404), por eso se busca ya filtrado por institutionProfileId
    Optional<InternshipAgreement> findByIdAndInstitutionProfileId(UUID id, UUID institutionProfileId);
    // Evita registrar dos convenios para la misma postulación
    boolean existsByJobApplicationId(UUID jobApplicationId);
}