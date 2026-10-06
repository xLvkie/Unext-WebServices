package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.PostulantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostulantProfileRepository extends JpaRepository<PostulantProfile, UUID> {
    Optional<PostulantProfile> findByUserId(UUID userId);
}