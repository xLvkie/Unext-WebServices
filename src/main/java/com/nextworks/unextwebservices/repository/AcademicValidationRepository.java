package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.AcademicValidation;
import com.nextworks.unextwebservices.entity.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicValidationRepository extends JpaRepository<AcademicValidation, UUID> {
    List<AcademicValidation> findByPostulantProfileId(UUID postulantProfileId);
    List<AcademicValidation> findByInstitutionProfileIdAndStatus(UUID institutionId, ValidationStatus status);
}
