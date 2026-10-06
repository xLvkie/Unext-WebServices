package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.dto.dashboard.TopCompanyDTO;
import com.nextworks.unextwebservices.entity.JobApplication;
import com.nextworks.unextwebservices.entity.enums.ApplicationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    boolean existsByJobOfferIdAndPostulantProfileId(UUID jobOfferId, UUID postulantProfileId);
    List<JobApplication> findByPostulantProfileId(UUID postulantProfileId);
    List<JobApplication> findByJobOfferIdOrderByCreatedAtDesc(UUID jobOfferId);

    // Ranking de empresas por alumnos distintos de la institucion con postulacion ACCEPTED
    @Query("SELECT new com.nextworks.unextwebservices.dto.dashboard.TopCompanyDTO(" +
            "r.id, r.companyName, COUNT(DISTINCT p.id), r.isValidated) " +
            "FROM JobApplication a JOIN a.jobOffer j JOIN j.recruiterProfile r JOIN a.postulantProfile p " +
            "WHERE p.institutionProfile.id = :institutionId AND a.status = :status " +
            "AND (:career = '' OR LOWER(TRIM(p.career)) = :career) " +
            "GROUP BY r.id, r.companyName, r.isValidated " +
            "ORDER BY COUNT(DISTINCT p.id) DESC, r.companyName ASC")
    List<TopCompanyDTO> findTopCompanies(@Param("institutionId") UUID institutionId, @Param("career") String career,
                                         @Param("status") ApplicationStatus status, Pageable pageable);
}
