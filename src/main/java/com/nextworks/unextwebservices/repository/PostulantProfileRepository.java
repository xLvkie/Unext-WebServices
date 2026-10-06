package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.dto.dashboard.CareerDemandDTO;
import com.nextworks.unextwebservices.entity.PostulantProfile;
import com.nextworks.unextwebservices.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostulantProfileRepository extends JpaRepository<PostulantProfile, UUID> {
    Optional<PostulantProfile> findByUserId(UUID userId);
    List<PostulantProfile> findByInstitutionProfileIdAndIsInstitutionVerifiedFalse(UUID institutionId);
    long countByInstitutionProfileIdAndIsInstitutionVerifiedTrue(UUID institutionId);
    long countByInstitutionProfileIdAndCareerIgnoreCase(UUID institutionId, String career);
    long countByInstitutionProfileIdAndIsInstitutionVerifiedTrueAndCareerIgnoreCase(UUID institutionId, String career);
    @Query("SELECT COUNT(DISTINCT p.id) FROM PostulantProfile p JOIN JobApplication a ON p.id = a.postulantProfile.id WHERE p.institutionProfile.id = :institutionId AND a.status = 'ACCEPTED'")
    long countHiredStudentsByInstitutionId(@Param("institutionId") UUID institutionId);

    // Dashboard: career llega ya normalizado (trim + minusculas); cadena vacia = sin filtro
    @Query("SELECT COUNT(p) FROM PostulantProfile p WHERE p.institutionProfile.id = :institutionId " +
            "AND (:career = '' OR LOWER(TRIM(p.career)) = :career)")
    long countStudentsByInstitutionAndCareer(@Param("institutionId") UUID institutionId, @Param("career") String career);

    @Query("SELECT COUNT(p) FROM PostulantProfile p WHERE p.institutionProfile.id = :institutionId " +
            "AND p.isInstitutionVerified = true AND (:career = '' OR LOWER(TRIM(p.career)) = :career)")
    long countVerifiedByInstitutionAndCareer(@Param("institutionId") UUID institutionId, @Param("career") String career);

    @Query("SELECT COUNT(DISTINCT p.id) FROM PostulantProfile p JOIN JobApplication a ON a.postulantProfile.id = p.id " +
            "WHERE p.institutionProfile.id = :institutionId AND a.status = :status " +
            "AND (:career = '' OR LOWER(TRIM(p.career)) = :career)")
    long countHiredByInstitutionAndCareer(@Param("institutionId") UUID institutionId, @Param("career") String career,
                                          @Param("status") ApplicationStatus status);

    // Demanda por carrera: alumnos sin carrera se agrupan como "Sin carrera"
    @Query("SELECT new com.nextworks.unextwebservices.dto.dashboard.CareerDemandDTO(" +
            "MIN(COALESCE(NULLIF(TRIM(p.career), ''), 'Sin carrera')), COUNT(a.id), " +
            "COALESCE(SUM(CASE WHEN a.status = :accepted THEN 1L ELSE 0L END), 0L)) " +
            "FROM PostulantProfile p LEFT JOIN JobApplication a ON a.postulantProfile.id = p.id " +
            "WHERE p.institutionProfile.id = :institutionId " +
            "GROUP BY LOWER(COALESCE(NULLIF(TRIM(p.career), ''), 'Sin carrera')) " +
            "ORDER BY COUNT(a.id) DESC")
    List<CareerDemandDTO> findDemandByCareer(@Param("institutionId") UUID institutionId,
                                             @Param("accepted") ApplicationStatus accepted);
}
