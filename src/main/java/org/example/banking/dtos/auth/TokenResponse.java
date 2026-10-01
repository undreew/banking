package org.example.banking.dtos.auth;

import org.example.banking.dtos.user.UserResponse;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {

    public static TokenResponse from(AuthenticationResponse result) {
        return new TokenResponse(
                result.accessToken(),
                "Bearer",
                result.accessTokenTtl().toSeconds(),
                result.user());
    }
}
