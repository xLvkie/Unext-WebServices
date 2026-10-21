package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.FeedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {
    // Feed general: del más reciente al más antiguo
    List<FeedPost> findAllByOrderByCreatedAtDesc();

    // Publicaciones de un usuario específico (para ver en su perfil)
    List<FeedPost> findByAuthorIdOrderByCreatedAtDesc(UUID authorUserId);
}