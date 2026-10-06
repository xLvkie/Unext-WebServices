package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import com.nextworks.unextwebservices.entity.InternshipAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InternshipAgreementRepository extends JpaRepository<InternshipAgreement, UUID> {
    // Listar todos los convenios de la institución
    List<InternshipAgreement> findByInstitutionProfileIdOrderByCreatedAtDesc(UUID institutionId);
    // Listar convenios filtrados por su estado
    List<InternshipAgreement> findByInstitutionProfileIdAndStatusOrderByCreatedAtDesc(UUID institutionId, AgreementStatus status);
    long countByInstitutionProfileIdAndStatus(UUID institutionId, AgreementStatus status);

    @Query("SELECT COUNT(a) FROM InternshipAgreement a WHERE a.institutionProfile.id = :institutionId AND a.status = :status AND LOWER(a.postulantProfile.career) = LOWER(:career)")
    long countByInstitutionAndStatusAndCareer(@Param("institutionId") UUID institutionId, @Param("status") AgreementStatus status, @Param("career") String career);
}
