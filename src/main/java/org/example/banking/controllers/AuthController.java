package org.example.banking.controllers;

import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.example.banking.dtos.auth.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.util.WebUtils;
import org.example.banking.configs.AppConfig;
import org.example.banking.constants.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import org.example.banking.services.AuthService;
import org.springframework.web.bind.annotation.*;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.validators.WithRateLimit;
import org.springframework.web.util.UriComponentsBuilder;
import org.example.banking.exceptions.UnauthenticatedException;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.AUTH)
public class AuthController {

    private final AuthService authService;
    private final AppConfig appConfig;
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedActor actor) {
        var user = authService.getCurrentUser(actor.id());
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PostMapping("/register")
    @WithRateLimit(limit = 3)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, UriComponentsBuilder uriBuilder) {
        var user = authService.register(request);
        return ResponseEntity.created(uriBuilder.path(ApiPaths.AUTH).build().toUri()).body(user);
    }

    @PostMapping("/login")
    @WithRateLimit(limit = 3)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var response = authService.login(request);
        return withRefreshCookie(response, response.refreshTokenTtl());
    }

    @PostMapping("/refresh")
    @WithRateLimit(limit = 10)
    public ResponseEntity<TokenResponse> refreshToken(HttpServletRequest request) {
        var response = authService.refreshToken(extractCookie(request));
        return withRefreshCookie(response, response.refreshTokenTtl());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var cookie =  extractCookie(request);
        if (cookie != null) {
            authService.logout(cookie);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    private String extractCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, appConfig.auth().refreshCookie());
        if (cookie == null || cookie.getValue().isBlank()) throw new UnauthenticatedException("Missing refresh cookie");
        return cookie.getValue();
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(AuthenticationResponse result, Duration ttl) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), ttl).toString())
                .body(TokenResponse.from(result));
    }

    private ResponseCookie refreshCookie(String value, Duration ttl) {
        return ResponseCookie.from(appConfig.auth().refreshCookie(), value)
                .httpOnly(true)
                .secure(appConfig.auth().cookieSecure())
                .sameSite("Strict")
                .path(ApiPaths.AUTH)
                .maxAge(ttl)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(appConfig.auth().refreshCookie(), "")
                .httpOnly(true)
                .secure(appConfig.auth().cookieSecure())
                .sameSite("Strict")
                .path(ApiPaths.AUTH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
