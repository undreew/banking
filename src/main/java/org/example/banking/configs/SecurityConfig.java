package org.example.banking.configs;

import jakarta.servlet.DispatcherType;
import org.example.banking.constants.ApiPaths;
import org.example.banking.filters.CookieOrBearerAuthenticationFilter;
import org.example.banking.filters.RateLimitFilter;
import org.example.banking.security.AccessDenylist;
import org.example.banking.security.BrowserRequestGuard;
import org.example.banking.security.ProblemDetailAuthHandlers;
import org.example.banking.services.TokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
// for using controller PreAuthorize()
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AppConfig appConfig,
            TokenService tokenService,
            AccessDenylist accessDenylist,
            ProblemDetailAuthHandlers authHandlers,
            BrowserRequestGuard browserRequestGuard,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        // Built here rather than as a @Bean so Spring Boot does not also register it as a
        // plain servlet filter outside the security chain.
        var authFilter = new CookieOrBearerAuthenticationFilter(
                tokenService,
                accessDenylist,
                browserRequestGuard,
                appConfig.auth().accessCookie()
        );

        http.sessionManagement(c ->
                        c.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )
                // Stateless API: no server session, so Spring's CSRF token repository has nothing to
                // bind to. Cookie-based authentication is only honored when BrowserRequestGuard passes
                // (custom header plus allow-listed Origin) and both cookies are SameSite=Strict.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // Lets the /error dispatch render 404s for unknown routes.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                ApiPaths.AUTH + "/register",
                                ApiPaths.AUTH + "/login",
                                ApiPaths.AUTH + "/refresh",
                                ApiPaths.AUTH + "/logout"
                        ).permitAll()
                        .requestMatchers(ApiPaths.ADMIN + "/**").hasRole("ADMIN")
                        .requestMatchers(ApiPaths.V1 + "/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(
                        authFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authHandlers)
                        .accessDeniedHandler(authHandlers)
                );

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitingFilter(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimitFilter);
        registration.addUrlPatterns("/*");
        return registration;
    }
}