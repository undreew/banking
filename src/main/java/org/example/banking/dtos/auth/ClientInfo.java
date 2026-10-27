package org.example.banking.dtos.auth;

import jakarta.servlet.http.HttpServletRequest;

public record ClientInfo(String userAgent, String ipAddress) {

    private static final int MAX_USER_AGENT = 255;

    public static ClientInfo from(HttpServletRequest request) {
        String agent = request.getHeader("User-Agent");
        if (agent != null && agent.length() > MAX_USER_AGENT) {
            agent = agent.substring(0, MAX_USER_AGENT);
        }
        // Behind a reverse proxy configure server.forward-headers-strategy so this is the client IP.
        return new ClientInfo(agent, request.getRemoteAddr());
    }
}