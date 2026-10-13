package org.example.banking.support;

import org.example.banking.configs.AppConfig;
import org.example.banking.configs.SecurityConfig;
import org.example.banking.services.TokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @WebMvcTest only loads the controller under test, not the app's own @Configuration
// classes, so @PreAuthorize is silently skipped unless SecurityConfig is explicitly
// imported — and SecurityConfig needs JwtAuthenticationFilter -> TokenService ->
// AppConfig, none of which the slice wires up on its own either.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, TokenService.class, MockMvcSecurityCustomization.class})
@EnableConfigurationProperties(AppConfig.class)
@ActiveProfiles("test")
public @interface WithRealSecurity {
}
