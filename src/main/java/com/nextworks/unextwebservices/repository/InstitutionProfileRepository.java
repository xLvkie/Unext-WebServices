package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.InstitutionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionProfileRepository extends JpaRepository<InstitutionProfile, UUID> {
    Optional<InstitutionProfile> findByUserId(UUID userId);
    boolean existsByDomain(String domain);
}