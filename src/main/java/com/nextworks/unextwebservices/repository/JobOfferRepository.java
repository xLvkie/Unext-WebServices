package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.enums.ExperienceLevel;
import com.nextworks.unextwebservices.entity.enums.JobModality;
import com.nextworks.unextwebservices.entity.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, UUID> {

    @Query("SELECT j FROM JobOffer j WHERE j.isActive = true " +
            "AND (:modality IS NULL OR j.modality = :modality) " +
            "AND (:experience IS NULL OR j.experienceLevel = :experience) " +
            "AND LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<JobOffer> searchActiveOffers(
            @Param("keyword") String keyword,
            @Param("modality") JobModality modality,
            @Param("experience") ExperienceLevel experience
    );
    List<JobOffer> findByRecruiterProfileIdOrderByCreatedAtDesc(UUID recruiterProfileId);
}