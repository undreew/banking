package org.example.banking.security;

import org.example.banking.configs.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Prints the effective security settings once at boot and refuses to start with relaxed
 * settings under the prod profile. A relaxed setting must be a visible deployment decision,
 * never something discovered during an incident.
 */
@Component
public class StartupSecurityCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupSecurityCheck.class);

    private final AppConfig config;
    private final Environment environment;

    public StartupSecurityCheck(AppConfig config, Environment environment) {
        this.config = config;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        var auth = config.auth();
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        log.info(
                "security: profiles={} accessTtl={} refreshTtl={} refreshAbsoluteTtl={} maxSessions={} "
                        + "accessCookie={} refreshCookie={} cookieSecure={} previousKey={} allowedOrigins={}",
                Arrays.toString(environment.getActiveProfiles()),
                auth.accessTokenTtl(),
                auth.refreshTokenTtl(),
                auth.refreshTokenAbsoluteTtl(),
                auth.maxSessions(),
                auth.accessCookie(),
                auth.refreshCookie(),
                auth.cookieSecure(),
                auth.hasPreviousSecret(),
                config.security().allowedOrigins().size()
        );

        if (!auth.cookieSecure()) {
            if (prod) {
                throw new IllegalStateException(
                        "banking.auth.cookie-secure must be true under the prod profile"
                );
            }

            log.warn("security: cookie-secure is FALSE. Acceptable for local http development only.");
        }
        
        if (prod && config.security().allowedOrigins().stream().anyMatch(o -> o.startsWith("http://"))) {
            throw new IllegalStateException(
                    "banking.security.allowed-origins must be https under the prod profile"
            );
        }
    }
}