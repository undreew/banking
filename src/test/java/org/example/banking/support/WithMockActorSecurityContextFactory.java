package org.example.banking.support;

import org.example.banking.dtos.auth.AuthenticatedActor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;
import java.util.UUID;

// Mirrors JwtAuthenticationFilter.authenticationFor() exactly: the principal is the
// raw AuthenticatedActor record, not a UserDetails. Plain @WithMockUser would make
// @AuthenticationPrincipal resolve to null instead of exercising the real 401/403 path.
public class WithMockActorSecurityContextFactory implements WithSecurityContextFactory<WithMockActor> {

    @Override
    public SecurityContext createSecurityContext(WithMockActor annotation) {
        UUID id = annotation.id().isBlank() ? UUID.randomUUID() : UUID.fromString(annotation.id());
        AuthenticatedActor actor = new AuthenticatedActor(id, annotation.email(), annotation.role());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                actor, null, List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
