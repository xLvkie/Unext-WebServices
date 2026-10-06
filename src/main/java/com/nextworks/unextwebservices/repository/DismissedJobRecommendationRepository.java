package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.DismissedJobRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DismissedJobRecommendationRepository extends JpaRepository<DismissedJobRecommendation, UUID> {
    boolean existsByPostulantProfileIdAndJobOfferId(UUID postulantProfileId, UUID jobOfferId);
    List<DismissedJobRecommendation> findByPostulantProfileId(UUID postulantProfileId);
}
