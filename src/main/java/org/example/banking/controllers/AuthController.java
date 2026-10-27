package org.example.banking.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.configs.AppConfig;
import org.example.banking.constants.ApiPaths;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.dtos.auth.ClientInfo;
import org.example.banking.dtos.auth.IssuedSession;
import org.example.banking.dtos.auth.LoginRequest;
import org.example.banking.dtos.auth.RegisterRequest;
import org.example.banking.dtos.auth.SessionResponse;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.exceptions.UnauthenticatedException;
import org.example.banking.security.CookieFactory;
import org.example.banking.security.PublicEndpoint;
import org.example.banking.services.AuthService;
import org.example.banking.validators.WithRateLimit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import java.util.Optional;

/**
 * Authentication endpoints. Tokens leave this controller only as Set-Cookie headers; every
 * body is a {@link SessionResponse} or a profile. The frontend never sees a token value.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.AUTH)
public class AuthController {

    private final AuthService authService;
    private final CookieFactory cookies;
    private final AppConfig appConfig;

    @PublicEndpoint(reason = "account creation happens before any credential exists")
    @WithRateLimit(limit = 3)
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        var user = authService.register(request);
        return ResponseEntity.created(uriBuilder.path(ApiPaths.AUTH).build().toUri())
                .body(user);
    }

    @PublicEndpoint(reason = "credential exchange")
    @WithRateLimit(limit = 5)
    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http
    ) {
        return withSessionCookies(authService.login(request, ClientInfo.from(http)));
    }

    @PublicEndpoint(reason = "the refresh cookie is the credential")
    @WithRateLimit(limit = 10)
    @PostMapping("/refresh")
    public ResponseEntity<SessionResponse> refresh(HttpServletRequest http) {
        String presented = refreshCookie(http)
                .orElseThrow(() -> new UnauthenticatedException("Missing refresh cookie"));

        return withSessionCookies(authService.refresh(presented, ClientInfo.from(http)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session(
            @AuthenticationPrincipal AuthenticatedActor actor
    ) {
        return ResponseEntity.ok(authService.session(actor));
    }

    @PublicEndpoint(reason = "must work after the access token expired; revokes whatever is presented")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal AuthenticatedActor actor,
            HttpServletRequest http
    ) {
        authService.logout(actor, refreshCookie(http).orElse(null), ClientInfo.from(http));
        return clearedCookies();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal AuthenticatedActor actor,
            HttpServletRequest http
    ) {
        authService.logoutAll(actor, ClientInfo.from(http));
        return clearedCookies();
    }

    private ResponseEntity<SessionResponse> withSessionCookies(IssuedSession issued) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookies.accessCookie(
                                issued.accessToken(),
                                issued.accessCookieMaxAge()
                        ).toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookies.refreshCookie(
                                issued.refreshToken(),
                                issued.refreshCookieMaxAge()
                        ).toString()
                )
                .body(issued.body());
    }

    private ResponseEntity<Void> clearedCookies() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.expiredAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.expiredRefreshCookie().toString())
                .build();
    }

    private Optional<String> refreshCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, appConfig.auth().refreshCookie());

        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(cookie.getValue());
    }
}