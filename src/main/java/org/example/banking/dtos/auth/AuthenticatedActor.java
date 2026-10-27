package org.example.banking.dtos.auth;

import org.example.banking.constants.accounts.Role;

import java.util.UUID;
import java.time.Instant;

/** * The security principal for one request, built from a verified access token. 
 * * Everything a controller or policy needs is here; nothing requires a database read.
 * */
public record AuthenticatedActor(
        UUID id,
        String role,
        UUID sessionId,
        UUID tokenId,
        Instant accessExpiresAt) {

    public boolean isAdmin() {
        return Role.ADMIN.name().equals(role);
    }
}
