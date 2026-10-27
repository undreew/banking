package org.example.banking.security;

import org.example.banking.configs.AppConfig;
import org.example.banking.constants.ApiPaths;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The only place that knows how the two auth cookies look.
 *
 * <ul>
 * <li>Access cookie: path {@code /}. It must reach the Next.js server on page navigations so
 * the route guard can redirect on presence; it is HttpOnly, so the wider path costs
 * nothing but a few hundred bytes per page request.</li>
 * <li>Refresh cookie: path {@value ApiPaths#AUTH}, sent only to the auth endpoints that need it.</li>
 * </ul>
 * Both are HttpOnly, SameSite=Strict and, outside local development, Secure. A name with the
 * {@code __Host-} prefix forces path {@code /}, as the browser requires.
 */
@Component
public class CookieFactory {

    private static final String HOST_PREFIX = "__Host-";
    private static final String ROOT_PATH = "/";

    private final AppConfig.Auth auth;

    public CookieFactory(AppConfig appConfig) {
        this.auth = appConfig.auth();
    }

    public ResponseCookie accessCookie(String value, Duration maxAge) {
        return build(auth.accessCookie(), value, ROOT_PATH, maxAge);
    }

    public ResponseCookie refreshCookie(String value, Duration maxAge) {
        return build(auth.refreshCookie(), value, ApiPaths.AUTH, maxAge);
    }

    public ResponseCookie expiredAccessCookie() {
        return build(auth.accessCookie(), "", ROOT_PATH, Duration.ZERO);
    }

    public ResponseCookie expiredRefreshCookie() {
        return build(auth.refreshCookie(), "", ApiPaths.AUTH, Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, String defaultPath, Duration maxAge) {
        String path = name.startsWith(HOST_PREFIX) ? ROOT_PATH : defaultPath;

        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(auth.cookieSecure())
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}