package org.example.banking.security;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a controller method that is intentionally reachable without authentication.
 * It has no runtime effect; the URL rule lives in {@code SecurityConfig}. Its purpose is to make
 * the exception explicit and to satisfy the ArchUnit rule that every endpoint declares its access.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
    String reason();
}