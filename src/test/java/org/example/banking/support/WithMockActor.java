package org.example.banking.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockActorSecurityContextFactory.class)
public @interface WithMockActor {
    String role() default "USER";

    String email() default "actor@example.com";

    // blank = a random id; set it to match a fixture's owner id for ownership checks
    String id() default "";
}
