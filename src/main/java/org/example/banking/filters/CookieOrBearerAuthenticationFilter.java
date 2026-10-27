package org.example.banking.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.security.AccessDenylist;
import org.example.banking.security.BrowserRequestGuard;
import org.example.banking.services.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Turns a credential into a security context. Two sources, in order of preference:
 * <ol>
 * <li>{@code Authorization: Bearer} for non-browser clients.</li>
 * <li>The access cookie, only when {@link BrowserRequestGuard} accepts the request.</li>
 * </ol>
 * A missing, invalid, expired or denylisted token leaves the request anonymous; the security
 * chain then answers 401. Nothing here throws for a bad token and nothing here logs its value.
 */
public class CookieOrBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CookieOrBearerAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final AccessDenylist accessDenylist;
    private final BrowserRequestGuard browserRequestGuard;
    private final String accessCookieName;

    public CookieOrBearerAuthenticationFilter(
            TokenService tokenService,
            AccessDenylist accessDenylist,
            BrowserRequestGuard browserRequestGuard,
            String accessCookieName
    ) {
        this.tokenService = tokenService;
        this.accessDenylist = accessDenylist;
        this.browserRequestGuard = browserRequestGuard;
        this.accessCookieName = accessCookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        bearerToken(request)
                .or(() -> cookieToken(request))
                .flatMap(tokenService::parseAccessToken)
                .filter(claims -> !accessDenylist.isDenied(claims.tokenId()))
                .map(tokenService::toActor)
                .ifPresentOrElse(
                        actor -> authenticate(actor, request),
                        () -> log.debug(
                                "no usable credential on {} {}",
                                request.getMethod(),
                                request.getRequestURI()
                        )
                );

        chain.doFilter(request, response);
    }

    private void authenticate(AuthenticatedActor actor, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + actor.role()))
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> bearerToken(HttpServletRequest request) {      
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private Optional<String> cookieToken(HttpServletRequest request) {
        if (!browserRequestGuard.allowsCookieAuth(request)) {
            return Optional.empty();
        }

        Cookie cookie = WebUtils.getCookie(request, accessCookieName);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(cookie.getValue());
    }
}