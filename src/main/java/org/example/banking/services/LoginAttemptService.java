package org.example.banking.services;

import lombok.RequiredArgsConstructor;
import org.example.banking.configs.AppConfig;
import org.example.banking.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a failed login in its own transaction so the counter is committed even though the
 * calling login transaction ends with an exception.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final AppConfig config;

    /** Returns true when this failure locked the account. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(UUID userId, Instant now) {
        AppConfig.Lockout lockout = config.auth().lockout();

        return userRepository.findById(userId)
                .map(user -> user.registerFailedLogin(
                        now,
                        lockout.failures(),
                        lockout.window(),
                        lockout.duration()
                ))
                .orElse(false);
    }
}