package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.enums.AgreementStatus;
import com.nextworks.unextwebservices.entity.InternshipAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
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
    // Convenio de otra institución: no se expone (404), por eso se busca ya filtrado por institutionProfileId
    Optional<InternshipAgreement> findByIdAndInstitutionProfileId(UUID id, UUID institutionProfileId);
    // Evita registrar dos convenios para la misma postulación
    boolean existsByJobApplicationId(UUID jobApplicationId);
}
