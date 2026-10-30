package com.nextworks.unextwebservices.repository;

import com.nextworks.unextwebservices.entity.UserConnection;
import com.nextworks.unextwebservices.entity.enums.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnection, UUID> {

    @Query("SELECT c FROM UserConnection c WHERE " +
            "(c.requester.id = :userA AND c.addressee.id = :userB) OR " +
            "(c.requester.id = :userB AND c.addressee.id = :userA)")
    Optional<UserConnection> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("SELECT c FROM UserConnection c WHERE " +
            "(c.requester.id = :userId OR c.addressee.id = :userId) AND c.status = :status")
    List<UserConnection> findAllByUserAndStatus(@Param("userId") UUID userId, @Param("status") ConnectionStatus status);

    List<UserConnection> findByAddresseeIdAndStatusOrderByCreatedAtDesc(UUID addresseeId, ConnectionStatus status);
}