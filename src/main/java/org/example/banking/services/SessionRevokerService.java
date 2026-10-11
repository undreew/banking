package org.example.banking.services;

import lombok.RequiredArgsConstructor;
import org.example.banking.repositories.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRevokerService {

    private static final Logger logger = LoggerFactory.getLogger(SessionRevokerService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    int revokeAllNow(UUID userId, Instant now) {
        int revoked = refreshTokenRepository.revokeAllForUser(userId.toString(), now);
        logger.warn("revoked {} live session(s) for user={} - committed independently of the caller", revoked, userId);
        return revoked;
    }
}
