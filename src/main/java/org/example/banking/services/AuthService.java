package org.example.banking.services;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.auth.TokenType;
import org.example.banking.entities.User;
import org.example.banking.mappers.AuthMapper;
import org.example.banking.mappers.UserMapper;
import org.example.banking.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.dtos.auth.LoginRequest;
import org.example.banking.dtos.auth.RegisterRequest;
import org.example.banking.repositories.AuthRepository;
import org.example.banking.dtos.auth.AuthenticationResponse;
import org.example.banking.exceptions.UserNotFoundException;
import org.example.banking.exceptions.UnauthenticatedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final PasswordEncoder passwordEncoder;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;

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

    @Transactional
    public AuthenticationResponse refreshToken(String presentedToken) {
        Claims claims = tokenService.parse(presentedToken, TokenType.REFRESH)
                .orElseThrow(UnauthenticatedException::sessionNotValid);

        UUID userId = UUID.fromString(claims.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(UnauthenticatedException::sessionNotValid);

        if (!user.isEnabled()) {
            throw new UnauthenticatedException("Account is disabled");
        }

        UUID successorId = UUID.randomUUID();

        return issueTokenPair(user, Instant.now(), successorId);
    }

    private AuthenticationResponse issueTokenPair(User user, Instant now) {
        return issueTokenPair(user, now, UUID.randomUUID());
    }

    private AuthenticationResponse issueTokenPair(User user, Instant now, UUID refreshTokenId) {
        logger.info("issued token pair for user={} issueAt={}", user.getId(), Date.from(now));

        return new AuthenticationResponse(
                tokenService.issueAccessToken(user, now),
                tokenService.issueRefreshToken(user.getId(), refreshTokenId, now),
                tokenService.getAccessTtl(),
                tokenService.getRefreshTtl(),
                userMapper.respond(user));
    }
}
