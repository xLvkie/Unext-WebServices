package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.PostulantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    long countHiredStudentsByInstitutionId(@org.springframework.data.repository.query.Param("institutionId") UUID institutionId);
    @Query("SELECT COUNT(DISTINCT p.id) FROM PostulantProfile p JOIN JobApplication a ON p.id = a.postulantProfile.id WHERE p.institutionProfile.id = :institutionId AND a.status = 'ACCEPTED' AND LOWER(p.career) = LOWER(:career)")
    long countHiredStudentsByInstitutionIdAndCareer(@org.springframework.data.repository.query.Param("institutionId") UUID institutionId, @org.springframework.data.repository.query.Param("career") String career);
}