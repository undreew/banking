package org.example.banking.repositories;

import org.example.banking.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "UPDATE refresh_tokens SET revoked_at = :now WHERE user_id = :userId AND revoked_at IS NULL")
    int revokeAllForUser(@Param("userId") String userId, @Param("now") Instant now);
}
