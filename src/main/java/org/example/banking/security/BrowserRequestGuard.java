package org.example.banking.security;

import jakarta.servlet.http.HttpServletRequest;
import org.example.banking.configs.AppConfig;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides whether a request may authenticate with the access cookie.
 *
 * <p>Two CSRF controls that are independent of the cookie's SameSite attribute:
 * <ol>
 * <li>The request must carry {@value #REQUIRED_HEADER}. HTML forms and cross-site navigation
 * cannot set custom headers, and a cross-origin script would need a CORS preflight that
 * this API rejects for unknown origins.</li>
 * <li>For state-changing methods the {@code Origin} header (or {@code Referer} when Origin is
 * absent) must match a configured allowed origin.</li>
 * </ol>
 * Requests that fail either check are treated as anonymous, never as errors, so a Bearer-header
 * client is unaffected.
 */
@Component
public class BrowserRequestGuard {

    public static final String REQUIRED_HEADER = "X-Requested-With";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final Set<String> allowedOrigins;

    public BrowserRequestGuard(AppConfig appConfig) {
        this.allowedOrigins = appConfig.security().allowedOrigins().stream()
                .map(BrowserRequestGuard::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean allowsCookieAuth(HttpServletRequest request) {
        String marker = request.getHeader(REQUIRED_HEADER);
        if (marker == null || marker.isBlank()) {
            return false;
        }

        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }

        String origin = originOf(request);
        return origin != null && allowedOrigins.contains(origin);
    }

    /** Returns the request's origin as {@code scheme://host[:port]}, or null when it cannot be determined. */
    static String originOf(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !"null".equalsIgnoreCase(origin.trim())) {
            return normalize(origin);
        }

        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(referer.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }

            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            return normalize(uri.getScheme() + "://" + uri.getHost() + port);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    static String normalize(String origin) {
        String value = origin.trim().toLowerCase(Locale.ROOT);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}