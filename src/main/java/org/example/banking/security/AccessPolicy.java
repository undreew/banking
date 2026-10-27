package org.example.banking.security;

import lombok.RequiredArgsConstructor;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.repositories.AccountRepository;
import org.example.banking.services.SessionService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Resource-level authorization in one place, referenced from {@code @PreAuthorize} as
 * {@code @access.ownsAccount(#id, principal)}. Services no longer need ad hoc ownership checks,
 * although defensive checks inside them do no harm.
 */
@Component("access")
@RequiredArgsConstructor
public class AccessPolicy {

    private final AccountRepository accountRepository;
    private final SessionService sessionService;

    /** Admins may act on any account; users only on accounts they own. */
    public boolean ownsAccount(UUID accountId, AuthenticatedActor actor) {
        if (actor == null || accountId == null) {
            return false;
        }

        if (actor.isAdmin()) {
            return true;
        }

        return accountRepository.findAccountByIdAndUserId(
                accountId.toString(),
                actor.id().toString()
        ).isPresent();
    }

    public boolean isSelf(UUID userId, AuthenticatedActor actor) {
        return actor != null && userId != null && userId.equals(actor.id());
    }

    /**
     * The access token is stateless and valid for its full lifetime. Money-moving endpoints add
     * this check so a session ended by logout-all or an administrator is refused immediately
     * rather than up to one access-token lifetime later.
     */
    public boolean sessionLive(AuthenticatedActor actor) {
        return actor != null && sessionService.isLive(actor.sessionId(), Instant.now());
    }
}