package org.example.banking.services;

import lombok.RequiredArgsConstructor;
import org.example.banking.entities.RefreshToken;
import org.example.banking.repositories.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Operations on a session chain. A chain starts at the row created by login and continues
 * through {@code replaced_by} on every refresh. The access token carries the id of the row
 * that was current when it was issued, so answering "is this session still live" means
 * following the chain to its head.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    // More hops than this inside one access-token lifetime means something is wrong.
    private static final int MAX_CHAIN_HOPS = 16;

    private final RefreshTokenRepository sessions;

    @Transactional(readOnly = true)
    public Optional<RefreshToken> head(UUID sessionId) {
        Optional<RefreshToken> current = sessions.findById(sessionId);
        int hops = 0;

        while (current.isPresent() && current.get().wasAlreadyRotated() && hops++ < MAX_CHAIN_HOPS) {
            UUID next = current.get().getReplacedBy();
            current = next == null ? Optional.empty() : sessions.findById(next);
        }

        return current;
    }

    @Transactional(readOnly = true)
    public boolean isLive(UUID sessionId, Instant now) {
        return head(sessionId).map(s -> s.isActive(now)).orElse(false);
    }

    /** Ends the session the given row belongs to, whichever row of the chain is presented. */
    @Transactional
    public void revokeChain(UUID sessionId, Instant now) {
        Optional<RefreshToken> current = sessions.findById(sessionId);
        int hops = 0;

        while (current.isPresent() && hops++ < MAX_CHAIN_HOPS) {
            RefreshToken row = current.get();
            boolean rotated = row.wasAlreadyRotated();
            row.revoke(now);

            if (!rotated || row.getReplacedBy() == null) {
                return;
            }

            current = sessions.findById(row.getReplacedBy());
        }
    }
}