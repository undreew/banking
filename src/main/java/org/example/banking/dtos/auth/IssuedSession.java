package org.example.banking.dtos.auth;

import java.time.Duration;

/** * Internal result of login and refresh. The controller turns the two token values into cookies 
 * * and returns only {@link #body()} to the client. 
 * */
public record IssuedSession(
        String accessToken,
        String refreshToken,
        Duration accessCookieMaxAge,
        Duration refreshCookieMaxAge,
        SessionResponse body) {
}