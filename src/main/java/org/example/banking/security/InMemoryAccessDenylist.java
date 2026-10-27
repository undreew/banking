package org.example.banking.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.banking.configs.AppConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class InMemoryAccessDenylist implements AccessDenylist {

    private final Cache<UUID, Instant> denied;

    public InMemoryAccessDenylist(AppConfig appConfig) {
        // No access token lives longer than its TTL plus the parser's clock skew, so entries
        // can be dropped after that regardless of their individual expiry.
        Duration retention = appConfig.auth().accessTokenTtl().plusMinutes(1);

        this.denied = Caffeine.newBuilder()
                .expireAfterWrite(retention)
                .maximumSize(200_000)
                .build();
    }

    @Override
    public void deny(UUID tokenId, Instant tokenExpiresAt) {
        if (tokenId == null) {
            return;
        }

        if (tokenExpiresAt != null && tokenExpiresAt.isBefore(Instant.now())) {
            return; // Already dead
        }

        denied.put(
                tokenId,
                tokenExpiresAt == null ? Instant.now() : tokenExpiresAt
        );
    }

    @Override
    public boolean isDenied(UUID tokenId) {
        return tokenId != null && denied.getIfPresent(tokenId) != null;
    }
}