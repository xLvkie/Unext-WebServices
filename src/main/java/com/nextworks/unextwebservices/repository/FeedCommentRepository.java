package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.FeedComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID> {
    // Lista los comentarios de un post en orden cronológico
    List<FeedComment> findByPostIdOrderByCreatedAtAsc(UUID postId);
    // Cuenta los comentarios para mostrarlo en el contador
    long countByPostId(UUID postId);
}