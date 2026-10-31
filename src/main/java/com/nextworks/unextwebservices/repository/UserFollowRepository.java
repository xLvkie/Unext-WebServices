package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {
    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
    Optional<UserFollow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
    List<UserFollow> findByFollowerIdOrderByCreatedAtDesc(UUID followerId);
    long countByFollowerId(UUID followerId);
}