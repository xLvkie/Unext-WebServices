package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // PANEL DERECHO: Historial completo entre dos usuarios ordenado cronológicamente
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
            "(m.sender.id = :userId2 AND m.receiver.id = :userId1) " +
            "ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    // PANEL IZQUIERDO: El último mensaje de cada conversación
    @Query(value = "SELECT DISTINCT ON (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id)) * " +
            "FROM messages " +
            "WHERE sender_id = :userId OR receiver_id = :userId " +
            "ORDER BY LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id), created_at DESC",
            nativeQuery = true)
    List<Message> findInboxForUser(@Param("userId") UUID userId);
}
