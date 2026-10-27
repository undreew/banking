package org.example.banking.configs;

import org.example.banking.security.BrowserRequestGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS is only relevant for browser clients on a different origin. The intended deployment
 * serves the frontend and the API from one origin through a Next.js rewrite, so in practice
 * this is a safety net for local development and for any explicitly configured extra origin.
 * Never "*": credentials are cookies.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppConfig appConfig) {
        var config = new CorsConfiguration();

        config.setAllowedOrigins(appConfig.security().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Idempotency-Key",
                "Authorization",
                BrowserRequestGuard.REQUIRED_HEADER
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}