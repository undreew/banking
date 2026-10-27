package org.example.banking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.banking.constants.errors.ErrorType;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Renders the security chain's own rejections (401 before a controller is reached, 403 from
 * method security) in the same RFC 9457 shape as {@code GlobalExceptionHandler}, so the frontend
 * has one error contract. Written by hand to avoid depending on a particular JSON library here.
 */
@Component
public class ProblemDetailAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        write(response, ErrorType.UNAUTHENTICATED, "Authentication is required");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        write(response, ErrorType.UNAUTHORIZED, "You do not have permission to perform this action");
    }

    private void write(
            HttpServletResponse response,
            ErrorType type,
            String detail
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(type.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String body = "{\"type\":\"about:blank\",\"title\":\"" + type.getSlug()
                + "\",\"status\":" + type.getStatus().value()
                + ",\"detail\":\"" + detail
                + "\",\"redirect\":false,\"timestamp\":\"" + Instant.now() + "\"}";

        response.getWriter().write(body);
    }
}