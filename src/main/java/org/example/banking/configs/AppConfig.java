package org.example.banking.configs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Typed, validated view of every {@code banking.*} property.
 * An invalid or missing value fails the boot instead of surfacing at the first request.
 */
@Validated
@ConfigurationProperties(prefix = "banking")
public record AppConfig(
        @NotNull @Valid Auth auth,
        @NotNull @Valid Security security
) {

    public record Auth(
            // NO DEFAULT. Generate with: openssl rand -base64 48
            @NotBlank(message = "banking.auth.secret must be set (env BANKING_JWT_SECRET)")
            @Size(min = 32, message = "the signing secret must be at least 32 characters")
            String secret,

            // Optional. Set only while rotating keys so tokens signed with the old key still verify.
            String previousSecret,

            @NotNull Duration accessTokenTtl,

            // Sliding lifetime: extended on every refresh.
            @NotNull Duration refreshTokenTtl,

            // Hard ceiling measured from login. A session can never outlive it.
            @NotNull Duration refreshTokenAbsoluteTtl,

            @NotBlank String accessCookie,
            @NotBlank String refreshCookie,
            @NotNull Boolean cookieSecure,

            // 0 means unlimited concurrent sessions per user.
            @Min(0) int maxSessions,

            @NotNull @Valid Lockout lockout
    ) {

        @AssertTrue(message = "banking.auth.access-token-ttl must be shorter than refresh-token-ttl")
        public boolean isAccessTokenShorterThanRefreshToken() {
            return accessTokenTtl != null
                    && refreshTokenTtl != null
                    && accessTokenTtl.compareTo(refreshTokenTtl) < 0;
        }

        @AssertTrue(message = "banking.auth.access-token-ttl must not exceed 1 hour")
        public boolean isAccessTokenBounded() {
            return accessTokenTtl != null
                    && accessTokenTtl.compareTo(Duration.ofHours(1)) <= 0;
        }

        @AssertTrue(message = "banking.auth.refresh-token-absolute-ttl must be at least refresh-token-ttl")
        public boolean isAbsoluteTtlAtLeastSliding() {
            return refreshTokenTtl != null
                    && refreshTokenAbsoluteTtl != null
                    && refreshTokenAbsoluteTtl.compareTo(refreshTokenTtl) >= 0;
        }

        @AssertTrue(message = "banking.auth.previous-secret must differ from banking.auth.secret")
        public boolean isPreviousSecretDifferent() {
            return !hasPreviousSecret() || !previousSecret.equals(secret);
        }

        public boolean hasPreviousSecret() {
            return previousSecret != null && !previousSecret.isBlank();
        }
    }

    public record Lockout(
            @Min(1) int failures,
            @NotNull Duration window,
            @NotNull Duration duration
    ) {
    }

    public record Security(
            // Full origins, e.g. https://app.example.com. Used for the CSRF Origin check and for CORS.
            @NotEmpty List<String> allowedOrigins
    ) {
    }
}