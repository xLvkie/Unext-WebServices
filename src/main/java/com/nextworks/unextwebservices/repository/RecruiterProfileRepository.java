package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, UUID> {
    Optional<RecruiterProfile> findByUserId(UUID userId);
    Optional<RecruiterProfile> findByIdAndIsVerifiedTrue(UUID id);
    boolean existsByRuc(String ruc);
    List<RecruiterProfile> findByIsVerifiedTrue();
    List<RecruiterProfile> findByIsVerifiedFalseAndCreatedAtBefore(LocalDateTime expirationDate);
}