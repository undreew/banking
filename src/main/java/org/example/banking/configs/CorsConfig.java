package org.example.banking.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${banking.cors.allowed-origins}") List<String> allowedOrigins) {
        var config = new CorsConfiguration();
        
        config.setAllowedOrigins(allowedOrigins); // never "*" — see RULES
        config.setAllowedMethods(List.of("GET","POST","PATCH","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type","Idempotency-Key","Authorization"));
        config.setAllowCredentials(true); // the refresh token is a cookie
        
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return source;
    }
}
