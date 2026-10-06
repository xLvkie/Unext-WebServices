package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.Review;
import com.nextworks.unextwebservices.entity.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByInternshipAgreementIdAndRecruiterProfileId(UUID agreementId, UUID recruiterProfileId);

    List<Review> findByPostulantProfileIdAndStatusOrderByCreatedAtDesc(UUID postulantProfileId, ReviewStatus status);

    List<Review> findByPostulantProfileIdAndStatusAndStarsOrderByCreatedAtDesc(
            UUID postulantProfileId, ReviewStatus status, Integer stars);
}
