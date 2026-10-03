package org.example.banking.dtos.auth;

import org.example.banking.dtos.user.UserResponse;

import java.time.Duration;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        UserResponse user) {
}
