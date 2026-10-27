package org.example.banking.dtos.auth;

import java.time.Instant;
import java.util.UUID;

/** The verified payload of an access token. */ 
public record AccessTokenClaims(
        UUID userId,
        UUID sessionId,
        UUID tokenId,
        String role,
        Instant expiresAt) {
}