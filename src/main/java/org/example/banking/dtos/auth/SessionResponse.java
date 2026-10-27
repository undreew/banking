package org.example.banking.dtos.auth;

import org.example.banking.dtos.user.UserResponse;

import java.time.Instant;
import java.util.UUID;

/** * The one body the frontend ever needs about its own session. Returned by login, refresh and 
 * * {@code GET /auth/session}. Contains no token. 
 * */
public record SessionResponse(
        UserResponse user,
        UUID sessionId,
        Instant accessExpiresAt,
        Instant refreshExpiresAt) {
}