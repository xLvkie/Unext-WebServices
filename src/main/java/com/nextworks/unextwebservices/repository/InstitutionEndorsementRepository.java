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
    // Busca la fila del par institucion-empresa sin importar si esta activa o revocada (para reactivarla)
    Optional<InstitutionEndorsement> findByInstitutionProfileIdAndRecruiterProfileId(UUID institutionId, UUID recruiterId);
    List<InstitutionEndorsement> findByInstitutionProfileId(UUID institutionId);
    long countByInstitutionProfileId(UUID institutionId);
    // Solo acreditaciones activas (no revocadas)
    List<InstitutionEndorsement> findByInstitutionProfileIdAndRevokedAtIsNull(UUID institutionId);
    // Cuantas instituciones acreditan activamente a esta empresa (para validationsCount/isValidated)
    long countByRecruiterProfileIdAndRevokedAtIsNull(UUID recruiterProfileId);
    long countByInstitutionProfileIdAndRevokedAtIsNull(UUID institutionId);
}
