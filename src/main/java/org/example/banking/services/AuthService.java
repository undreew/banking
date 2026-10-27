package org.example.banking.services;

import lombok.RequiredArgsConstructor;
import org.example.banking.configs.AppConfig;
import org.example.banking.constants.audit.AuditAction;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.dtos.auth.ClientInfo;
import org.example.banking.dtos.auth.IssuedSession;
import org.example.banking.dtos.auth.LoginRequest;
import org.example.banking.dtos.auth.RegisterRequest;
import org.example.banking.dtos.auth.SessionResponse;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.entities.RefreshToken;
import org.example.banking.entities.User;
import org.example.banking.exceptions.UnauthenticatedException;
import org.example.banking.mappers.UserMapper;
import org.example.banking.repositories.AuthRepository;
import org.example.banking.repositories.RefreshTokenRepository;
import org.example.banking.repositories.UserRepository;
import org.example.banking.security.AccessDenylist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Login, refresh, logout and session reporting. Every token value is created here and handed to
 * the controller inside an {@link IssuedSession}; nothing in this class touches HTTP.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String TARGET_USER = "User";
    private static final String TARGET_SESSION = "Session";

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository sessions;
    private final SessionService sessionService;
    private final LoginAttemptService loginAttemptService;
    private final AccessDenylist accessDenylist;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final AppConfig config;

    public UserResponse register(RegisterRequest request) {
        var user = authRepository.save(User.create(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                passwordEncoder.encode(request.getPassword())
        ));

        logger.info("user registered id={}", user.getId());
        return userMapper.respond(user);
    }

    /**
     * Credentials in, session out. Every failure path returns the same generic message so the
     * response does not reveal whether the email exists, the password was wrong or the account is
     * locked. Failures are counted in a separate transaction (see {@link LoginAttemptService}).
     */
    @Transactional
    public IssuedSession login(LoginRequest request, ClientInfo client) {
        Instant now = Instant.now();
        User user = authRepository.findByEmail(request.getEmail())
                .orElseThrow(UnauthenticatedException::new);

        if (user.isLocked(now)) {
            auditService.record(
                    user.getId(),
                    AuditAction.LOGIN_FAILURE,
                    TARGET_USER,
                    user.getId(),
                    "attempt while locked",
                    client.ipAddress()
            );
            throw new UnauthenticatedException();
        }

        if (!user.isEnabled()) {
            throw new UnauthenticatedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            boolean locked = loginAttemptService.recordFailure(user.getId(), now);
            auditService.record(
                    user.getId(),
                    locked ? AuditAction.ACCOUNT_LOCKED : AuditAction.LOGIN_FAILURE,
                    TARGET_USER,
                    user.getId(),
                    locked ? "locked after repeated failures" : "wrong password",
                    client.ipAddress()
            );
            throw new UnauthenticatedException();
        }

        user.clearFailedLogins();
        enforceSessionCap(user.getId(), now);
        IssuedSession issued = openSession(user, now, client);

        auditService.record(
                user.getId(),
                AuditAction.LOGIN_SUCCESS,
                TARGET_SESSION,
                issued.body().sessionId(),
                null,
                client.ipAddress()
        );

        logger.info("login ok user={} session={}", user.getId(), issued.body().sessionId());
        return issued;
    }

    /**
     * Rotation with reuse detection. The presented value is looked up by hash under a row lock.
     * A value that was already rotated means it leaked and was replayed: every session of the
     * user is revoked and the event is recorded.
     * <p>
     * The revocation runs in this same transaction. A separate REQUIRES_NEW transaction would
     * block on the row lock this transaction already holds. The method then ends with a 401, so
     * {@code noRollbackFor} keeps the revocation committed instead of rolling it back.
     */
    @Transactional(noRollbackFor = UnauthenticatedException.class)
    public IssuedSession refresh(String presentedToken, ClientInfo client) {
        Instant now = Instant.now();
        RefreshToken session = sessions.findByTokenHashForUpdate(
                tokenService.hashRefreshToken(presentedToken)
        ).orElseThrow(UnauthenticatedException::sessionNotValid);

        if (session.wasAlreadyRotated()) {
            int revoked = sessions.revokeAllForUser(session.getUserId().toString(), now);
            auditService.record(
                    session.getUserId(),
                    AuditAction.REFRESH_REUSE_DETECTED,
                    TARGET_SESSION,
                    session.getId(),
                    "revoked " + revoked + " session(s)",
                    client.ipAddress()
            );
            logger.warn(
                    "refresh token REUSE detected user={} session={} revoked={}",
                    session.getUserId(),
                    session.getId(),
                    revoked
            );
            throw UnauthenticatedException.sessionNotValid();
        }

        if (!session.isActive(now)) {
            throw UnauthenticatedException.sessionNotValid();
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(UnauthenticatedException::sessionNotValid);

        if (!user.isEnabled()) {
            throw new UnauthenticatedException("Account is disabled");
        }

        UUID successorId = UUID.randomUUID();
        String refreshValue = tokenService.newRefreshTokenValue();
        Instant expiresAt = earliest(
                now.plus(tokenService.getRefreshTtl()),
                session.getAbsoluteExpiresAt()
        );

        RefreshToken successor = RefreshToken.issue(
                successorId,
                user.getId(),
                tokenService.hashRefreshToken(refreshValue),
                expiresAt,
                session.getAbsoluteExpiresAt(),
                client
        );

        session.rotateTo(successorId, now);
        sessions.save(successor);

        auditService.record(
                user.getId(),
                AuditAction.TOKEN_REFRESHED,
                TARGET_SESSION,
                successorId,
                null,
                client.ipAddress()
        );

        return issue(user, successor, refreshValue, now);
    }

    /**
     * Works with or without a valid access token so a user whose access token already expired can
     * still end the session. Whatever is presented is revoked; the access token id is denylisted
     * until it would have expired.
     */
    @Transactional
    public void logout(AuthenticatedActor actor, String presentedRefreshToken, ClientInfo client) {
        Instant now = Instant.now();

        if (presentedRefreshToken != null && !presentedRefreshToken.isBlank()) {
            sessions.findByTokenHash(tokenService.hashRefreshToken(presentedRefreshToken))
                    .ifPresent(session -> {
                        sessionService.revokeChain(session.getId(), now);
                        auditService.record(
                                session.getUserId(),
                                AuditAction.LOGOUT,
                                TARGET_SESSION,
                                session.getId(),
                                null,
                                client.ipAddress()
                        );
                    });
        }

        if (actor != null) {
            sessionService.revokeChain(actor.sessionId(), now);
            accessDenylist.deny(actor.tokenId(), actor.accessExpiresAt());
        }
    }

    @Transactional
    public void logoutAll(AuthenticatedActor actor, ClientInfo client) {
        int revoked = sessions.revokeAllForUser(actor.id().toString(), Instant.now());
        accessDenylist.deny(actor.tokenId(), actor.accessExpiresAt());

        auditService.record(
                actor.id(),
                AuditAction.LOGOUT_ALL,
                TARGET_USER,
                actor.id(),
                "revoked " + revoked + " session(s)",
                client.ipAddress()
        );
    }

    /** What the frontend polls. Fails with 401 once the session chain is no longer live. */
    @Transactional(readOnly = true)
    public SessionResponse session(AuthenticatedActor actor) {
        Instant now = Instant.now();
        User user = userRepository.findById(actor.id())
                .orElseThrow(UnauthenticatedException::sessionNotValid);

        RefreshToken head = sessionService.head(actor.sessionId())
                .filter(s -> s.isActive(now))
                .orElseThrow(UnauthenticatedException::sessionNotValid);

        return new SessionResponse(
                userMapper.respond(user),
                actor.sessionId(),
                actor.accessExpiresAt(),
                head.effectiveExpiry()
        );
    }

    private IssuedSession openSession(User user, Instant now, ClientInfo client) {
        UUID sessionId = UUID.randomUUID();
        String refreshValue = tokenService.newRefreshTokenValue();
        Instant absoluteExpiry = now.plus(tokenService.getRefreshAbsoluteTtl());
        Instant expiresAt = earliest(now.plus(tokenService.getRefreshTtl()), absoluteExpiry);

        RefreshToken session = RefreshToken.issue(
                sessionId,
                user.getId(),
                tokenService.hashRefreshToken(refreshValue),
                expiresAt,
                absoluteExpiry,
                client
        );

        sessions.save(session);
        return issue(user, session, refreshValue, now);
    }

    private IssuedSession issue(
            User user,
            RefreshToken session,
            String refreshValue,
            Instant now
    ) {
        Instant accessExpiresAt = now.plus(tokenService.getAccessTtl());
        String accessToken = tokenService.issueAccessToken(
                user,
                session.getId(),
                UUID.randomUUID(),
                now
        );

        Duration refreshCookieMaxAge = Duration.between(now, session.effectiveExpiry());

        SessionResponse body = new SessionResponse(
                userMapper.respond(user),
                session.getId(),
                accessExpiresAt,
                session.effectiveExpiry()
        );

        return new IssuedSession(
                accessToken,
                refreshValue,
                tokenService.getAccessTtl(),
                refreshCookieMaxAge,
                body
        );
    }

    /** With banking.auth.max-sessions > 0, the oldest sessions are revoked to make room for the new one. */
    private void enforceSessionCap(UUID userId, Instant now) {
        int max = config.auth().maxSessions();
        if (max <= 0) {
            return;
        }

        List<RefreshToken> active = sessions.findActiveByUser(userId, now);
        int toRevoke = active.size() - max + 1;

        for (int i = 0; i < toRevoke && i < active.size(); i++) {
            active.get(i).revoke(now);
            auditService.record(
                    userId,
                    AuditAction.SESSION_REVOKED,
                    TARGET_SESSION,
                    active.get(i).getId(),
                    "session cap " + max,
                    null
            );
        }
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}