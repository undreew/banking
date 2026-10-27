package org.example.banking.security;

import java.time.Instant;
import java.util.UUID;

public interface AccessDenylist {

    void deny(UUID tokenId, Instant tokenExpiresAt);

    boolean isDenied(UUID tokenId);
}