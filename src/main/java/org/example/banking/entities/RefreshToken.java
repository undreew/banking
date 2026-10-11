package org.example.banking.entities;

import lombok.Getter;
import jakarta.persistence.*;
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

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "replaced_by", columnDefinition = "BINARY(16)")
    private UUID replacedBy;

    protected RefreshToken() {
    }

    public static RefreshToken issue(UUID id, UUID userId, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.id = Objects.requireNonNull(id, "id");
        token.userId = Objects.requireNonNull(userId, "userId");
        token.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        return token;
    }

    public boolean wasAlreadyRotated() {
        return replacedBy != null;
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void rotateTo(UUID successor, Instant at) {
        this.revokedAt = at;
        this.replacedBy = Objects.requireNonNull(successor, "successor");
    }

    public void revoke(Instant at) {
        if (this.revokedAt == null) {
            this.revokedAt = at;
        }
    }
}
