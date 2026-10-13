package org.example.banking.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

// Spring Boot 4.1's @WebMvcTest does not yet auto-apply Spring Security's MockMvc
// support even with spring-security-test on the classpath, so without this, a
// @WithMockActor-populated SecurityContext never reaches the request: Spring
// Security's own SecurityContextHolderFilter resets the holder to empty at the start
// of every filter chain run, and only springSecurity()'s request post-processor knows
// how to put the test's context back before that filter runs.
@TestConfiguration
public class MockMvcSecurityCustomization {

    @Bean
    MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
        return builder -> builder.apply(SecurityMockMvcConfigurers.springSecurity());
    }
}
