package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    boolean existsByJobOfferIdAndPostulantProfileId(UUID jobOfferId, UUID postulantProfileId);
}
