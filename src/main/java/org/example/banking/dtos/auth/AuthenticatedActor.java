package org.example.banking.dtos.auth;

import java.util.UUID;

public record AuthenticatedActor(
        UUID id,
        String email,
        String role) {
}
