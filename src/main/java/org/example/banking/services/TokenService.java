package org.example.banking.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.example.banking.configs.AppConfig;
import org.example.banking.constants.auth.TokenType;
import org.example.banking.dtos.auth.AccessTokenClaims;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.entities.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies access tokens and generates refresh values.
 *
 * <p>Access token: HMAC-SHA256 JWT with a {@code kid} header so a previous key can keep verifying
 * during rotation. Claims are the minimum needed to authorise a request: subject, role, session id,
 * token id, type and validity window. No email, no PII.
 *
 * <p>Refresh token: 32 random bytes, base64url. Never a JWT. The database stores only its hash.
 */
@Service
public class TokenService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_SESSION = "sid";
    private static final int KID_HEX_CHARS = 16;
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SecretKey currentKey;
    private final String currentKid;
    private final SecretKey previousKey; // null unless a rotation is in progress
    private final String previousKid; // null unless a rotation is in progress
    private final Locator<Key> keyLocator;
    private final SecureRandom secureRandom = new SecureRandom();

    @Getter
    private final Duration accessTtl;

    @Getter
    private final Duration refreshTtl;

    @Getter
    private final Duration refreshAbsoluteTtl;

    public TokenService(AppConfig config) {
        AppConfig.Auth auth = config.auth();
        this.currentKey = Keys.hmacShaKeyFor(auth.secret().getBytes(StandardCharsets.UTF_8));
        this.currentKid = keyIdOf(auth.secret());
        this.previousKey = auth.hasPreviousSecret()
                ? Keys.hmacShaKeyFor(auth.previousSecret().getBytes(StandardCharsets.UTF_8))
                : null;
        this.previousKid = auth.hasPreviousSecret() ? keyIdOf(auth.previousSecret()) : null;
        this.accessTtl = auth.accessTokenTtl();
        this.refreshTtl = auth.refreshTokenTtl();
        this.refreshAbsoluteTtl = auth.refreshTokenAbsoluteTtl();

        // The kid identifies the key material, not its role, so a token signed before a rotation
        // still names the key that signed it. An unknown kid falls back to the current key, which
        // then fails signature verification in the normal way.
        this.keyLocator = new LocatorAdapter<>() {
            @Override
            protected Key locate(ProtectedHeader header) {
                boolean previous = previousKey != null && previousKid.equals(header.getKeyId());
                return previous ? previousKey : currentKey;
            }
        };
    }

    public String issueAccessToken(User user, UUID sessionId, UUID tokenId, Instant now) {
        return Jwts.builder()
                .header().keyId(currentKid).and()
                .id(tokenId.toString())
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, TokenType.ACCESS.name())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_SESSION, sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(currentKey)
                .compact();
    }

    /** A fresh opaque refresh value. Only its hash is ever persisted. */
    public String newRefreshTokenValue() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String value) {
        return sha256Hex(value);
    }

    /** Public, non-secret identifier of a signing key: the first 16 hex chars of its SHA-256. */
    static String keyIdOf(String secret) {
        return sha256Hex(secret).substring(0, KID_HEX_CHARS);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the JVM specification", ex);
        }
    }

    /** Verifies signature, type and validity window. Empty on any failure; never throws. */
    public Optional<AccessTokenClaims> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(keyLocator)
                    .clockSkewSeconds(CLOCK_SKEW.toSeconds())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TokenType.ACCESS.name().equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }

            return Optional.of(new AccessTokenClaims(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get(CLAIM_SESSION, String.class)),
                    UUID.fromString(claims.getId()),
                    claims.get(CLAIM_ROLE, String.class),
                    claims.getExpiration().toInstant()
            ));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    public AuthenticatedActor toActor(AccessTokenClaims claims) {
        return new AuthenticatedActor(
                claims.userId(),
                claims.role(),
                claims.sessionId(),
                claims.tokenId(),
                claims.expiresAt()
        );
    }
}