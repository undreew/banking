package org.example.banking.configs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "banking")
public record AppConfig(
        @NotNull @Valid Auth auth
) {
    public record Auth(
            @NotBlank(message = "banking.auth.secret must be set (env BANKING_JWT_SECRET)")
            @Size(min = 32, message = "the signing secret must be at least 32 characters")
            String secret,

            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl,
            @NotNull Boolean cookieSecure,
            @NotNull String refreshCookie) {

        /**
         * A short access token is only meaningful if it is much shorter than the refresh token;
         */
        @AssertTrue(message = "banking.auth.access-token-ttl must be shorter than refresh-token-ttl")
        public boolean isAccessTokenShorterThanRefreshToken() {
            return accessTokenTtl != null && refreshTokenTtl != null
                    && accessTokenTtl.compareTo(refreshTokenTtl) < 0;
        }

        /**
         * Hard ceiling: a "short" access token measured in hours is not short.
         */
        @AssertTrue(message = "banking.auth.access-token-ttl must not exceed 1 hour")
        public boolean isAccessTokenBounded() {
            return accessTokenTtl != null && accessTokenTtl.compareTo(Duration.ofHours(1)) <= 0;
        }
    }
}
