package org.example.banking.services;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.auth.TokenType;
import org.example.banking.dtos.auth.*;
import org.example.banking.entities.RefreshToken;
import org.example.banking.entities.User;
import org.example.banking.exceptions.ConflictException;
import org.example.banking.exceptions.NotFoundException;
import org.example.banking.mappers.AuthMapper;
import org.example.banking.mappers.UserMapper;
import org.example.banking.repositories.RefreshTokenRepository;
import org.example.banking.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.repositories.AuthRepository;
import org.example.banking.exceptions.UnauthenticatedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRevokerService sessionRevokerService;
    private final PasswordEncoder passwordEncoder;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;

    public UserResponse getCurrentUser(UUID actorId) {
        var user = authRepository.findById(actorId).orElseThrow(() -> NotFoundException.of("Account", actorId));
        return authMapper.toView(user);
    }

    public UserResponse register(RegisterRequest request) {
        var user = authRepository.save(User.create(request.getEmail(), request.getFirstName(), request.getLastName(), passwordEncoder.encode(request.getPassword())));
        logger.info("user registered id={}", user.getId());
        return authMapper.toView(user);
    }

    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        Optional<User> foundUser = authRepository.findByEmail(request.getEmail());
        User user = foundUser.orElseThrow(UnauthenticatedException::new);

        if (!user.isEnabled()) {
            throw new UnauthenticatedException("Account is disabled");
        }

        boolean isMatch = foundUser
                .map(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElse(false);

        if (!isMatch) {
            throw new UnauthenticatedException();
        }

        return issueTokenPair(user, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuthenticationResponse refreshToken(String presentedToken) {
        Instant now = Instant.now();

        Claims claims = tokenService.parse(presentedToken, TokenType.REFRESH).orElseThrow(UnauthenticatedException::sessionNotValid);

        UUID tokenId = UUID.fromString(claims.getId());

        RefreshToken token = refreshTokenRepository.findById(tokenId).orElseThrow(UnauthenticatedException::sessionNotValid);

        if (token.wasAlreadyRotated()) {
            logger.info("token userId={}", token.getUserId());
            int revoked = sessionRevokerService.revokeAllNow(token.getUserId(), now);
            logger.warn("refresh token REUSE detected for user={} token={} - revoked {} live sessions", token.getUserId(), tokenId, revoked);
            throw UnauthenticatedException.sessionNotValid();
        }

        if (!token.isUsable(now)) {
            throw UnauthenticatedException.sessionNotValid();
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(UnauthenticatedException::sessionNotValid);

        if (!user.isEnabled()) {
            throw new UnauthenticatedException("Account is disabled");
        }

        UUID successorId = UUID.randomUUID();
        token.rotateTo(successorId, now);

        return issueTokenPair(user, now, successorId);
    }

    @Transactional
    public void logout(String token) {
        tokenService.parse(token, TokenType.REFRESH)
                .map(Claims::getId)
                .map(UUID::fromString)
                .flatMap(refreshTokenRepository::findById)
                .ifPresent(refreshToken -> {
                    refreshToken.revoke(Instant.now());
                    logger.info("logout: session {} revoked for user={}", refreshToken.getId(), refreshToken.getUserId());
                });
    }

    @Transactional
    public void changePassword(UUID id, @Valid ChangePasswordRequest request) {
        var user = authRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
        var isOldMatch = comparePasswords(request.getOldPassword(), user.getPassword());
        if (!isOldMatch) {
            throw new ConflictException("Old passwords don't match");
        }
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        authRepository.save(user);
    }

    private AuthenticationResponse issueTokenPair(User user, Instant now) {
        return issueTokenPair(user, now, UUID.randomUUID());
    }

    private AuthenticationResponse issueTokenPair(User user, Instant now, UUID refreshTokenId) {
        var token = RefreshToken.issue(refreshTokenId, user.getId(), now.plus(tokenService.getRefreshTtl()));
        logger.info("refresh token issued refreshToken={}", token);

        refreshTokenRepository.save(token);

        logger.info("issued token pair for user={} issueAt={}", user.getId(), Date.from(now));

        return new AuthenticationResponse(
                tokenService.issueAccessToken(user, now),
                tokenService.issueRefreshToken(user.getId(), refreshTokenId, now),
                tokenService.getAccessTtl(),
                tokenService.getRefreshTtl(),
                userMapper.respond(user));
    }

    private boolean comparePasswords(String stringPassword, String bcryptPassword) {
        return passwordEncoder.matches(stringPassword, bcryptPassword);
    }
}
