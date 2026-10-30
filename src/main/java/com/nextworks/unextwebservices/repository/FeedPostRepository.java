package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.FeedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {
    // Feed general: del más reciente al más antiguo
    List<FeedPost> findAllByOrderByCreatedAtDesc();

    // Publicaciones de un usuario específico (para ver en su perfil)
    List<FeedPost> findByAuthorIdOrderByCreatedAtDesc(UUID authorUserId);

    // Publicaciones importantes fijadas por un usuario (para la card lateral de su perfil)
    List<FeedPost> findByAuthorIdAndIsImportantTrueOrderByCreatedAtDesc(UUID authorUserId);

    // Contador para validar el límite según el rol (Postulante: 3, Reclutador: 5, Institución: 6)
    long countByAuthorIdAndIsImportantTrue(UUID authorUserId);

    // Elimina las publicaciones normales cuyo tiempo de vida ya expiró
    @Modifying
    @Query("DELETE FROM FeedPost p WHERE p.isImportant = false AND p.expiresAt IS NOT NULL AND p.expiresAt <= :now")
    void deleteExpiredPosts(@Param("now") LocalDateTime now);
}