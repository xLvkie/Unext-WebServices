package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.InstitutionEndorsement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionEndorsementRepository extends JpaRepository<InstitutionEndorsement, UUID> {
    boolean existsByInstitutionProfileIdAndRecruiterProfileId(UUID institutionId, UUID recruiterId);
    Optional<InstitutionEndorsement> findByInstitutionProfileIdAndRecruiterProfileId(UUID institutionId, UUID recruiterId);
    List<InstitutionEndorsement> findByInstitutionProfileId(UUID institutionId);
    long countByInstitutionProfileId(UUID institutionId);
}
