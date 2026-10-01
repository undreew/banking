package org.example.banking.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.example.banking.entities.User;
import org.example.banking.configs.AppConfig;
import org.springframework.stereotype.Service;
import org.example.banking.constants.auth.TokenType;
import org.example.banking.dtos.auth.AuthenticatedActor;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Getter
@Service
public class TokenService {
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public TokenService(AppConfig config) {
        this.signingKey = Keys.hmacShaKeyFor(config.auth().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = config.auth().accessTokenTtl();
        this.refreshTtl = config.auth().refreshTokenTtl();
    }

    public String issueRefreshToken(UUID userId, UUID tokenId, Instant now) {
        return Jwts.builder()
                .id(tokenId.toString())
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(signingKey)
                .compact();
    }

    public String issueAccessToken(User user, Instant now) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, TokenType.ACCESS.name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey)
                .compact();
    }

    public Optional<Claims> parse(String token, TokenType type) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clockSkewSeconds(CLOCK_SKEW.toSeconds())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!type.name().equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public AuthenticatedActor toActor(Claims claims) {
        return new AuthenticatedActor(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                claims.get(CLAIM_ROLE, String.class));
    }
}
