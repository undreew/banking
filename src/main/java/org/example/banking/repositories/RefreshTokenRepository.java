package org.example.banking.repositories;

import jakarta.persistence.LockModeType;
import org.example.banking.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Pessimistic lock so two concurrent refreshes with the same value are serialised:
     * the second one then sees ROTATED and is treated as reuse instead of producing two
     * successors.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RefreshToken s where s.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("select s from RefreshToken s where s.userId = :userId "
            + "and s.status = org.example.banking.constants.auth.SessionStatus.ACTIVE "
            + "and s.expiresAt > :now order by s.expiresAt asc")
    List<RefreshToken> findActiveByUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            nativeQuery = true,
            value = "UPDATE refresh_tokens SET status = 'REVOKED', revoked_at = :now "
                    + "WHERE user_id = :userId AND status <> 'REVOKED'"
    )
    int revokeAllForUser(
            @Param("userId") String userId,
            @Param("now") Instant now
    );

    @Modifying
    @Query(
            nativeQuery = true,
            value = "DELETE FROM refresh_tokens WHERE expires_at < :cutoff LIMIT :batch"
    )
    int deleteExpiredBefore(
            @Param("cutoff") Instant cutoff,
            @Param("batch") int batch
    );
}