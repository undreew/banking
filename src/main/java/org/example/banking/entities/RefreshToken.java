package org.example.banking.entities;

import lombok.Getter;
import jakarta.persistence.*;
import org.example.banking.constants.auth.SessionStatus;
import org.example.banking.dtos.auth.ClientInfo;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;
import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SessionStatus status;

    // Sliding expiry of this refresh value.
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    // Hard ceiling of the whole chain, copied unchanged to every successor.
    @Column(name = "absolute_expires_at", nullable = false, updatable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "replaced_by", columnDefinition = "BINARY(16)")
    private UUID replacedBy;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected RefreshToken() {
    }

    public static RefreshToken issue(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant absoluteExpiresAt,
            ClientInfo client
    ) {
        RefreshToken token = new RefreshToken();
        token.id = Objects.requireNonNull(id, "id");
        token.userId = Objects.requireNonNull(userId, "userId");
        token.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        token.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        token.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt");
        token.status = SessionStatus.ACTIVE;

        if (client != null) {
            token.userAgent = client.userAgent();
            token.ipAddress = client.ipAddress();
        }

        return token;
    }
    
    public boolean isActive(Instant now) {
        return status == SessionStatus.ACTIVE && expiresAt.isAfter(now) && absoluteExpiresAt.isAfter(now);
    }

    public boolean wasAlreadyRotated() {
        return status == SessionStatus.ROTATED;
    }

    public boolean isRevoked() {
        return status == SessionStatus.REVOKED;
    }
    
    public Instant effectiveExpiry() {
        return expiresAt.isBefore(absoluteExpiresAt) ? expiresAt : absoluteExpiresAt;
    }

    public void rotateTo(UUID successor, Instant at) {
        this.status = SessionStatus.ROTATED;
        this.revokedAt = at;
        this.lastUsedAt = at;
        this.replacedBy = Objects.requireNonNull(successor, "successor");
    }

    public void revoke(Instant at) {
        if (this.status != SessionStatus.REVOKED) {
            this.status = SessionStatus.REVOKED;
            this.revokedAt = at;
        }
    }
}
