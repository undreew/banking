package org.example.banking.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.web.method.HandlerMethod;
import org.example.banking.validators.WithRateLimit;
import org.example.banking.exceptions.RateLimitException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
//@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RequestMappingHandlerMapping handlerMapping;
    private final HandlerExceptionResolver resolver;

    public RateLimitFilter(RequestMappingHandlerMapping handlerMapping, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.handlerMapping = handlerMapping;
        this.resolver = resolver;
    }

    // A thread-safe cache that automatically purges entries 1 minute after creation
    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10_000) // Safeguards memory against DDoS attacks
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain)
            throws ServletException, IOException {

        int allowedLimit = -1;
        boolean redirect = false;

        try {
            // Resolve the handler using the explicit mapping engine
            HandlerExecutionChain mapping = handlerMapping.getHandler(httpRequest);

            if (mapping != null && mapping.getHandler() instanceof HandlerMethod handlerMethod) {

                // Extract the actual annotation instance from the method
                WithRateLimit annotation = handlerMethod.getMethodAnnotation(WithRateLimit.class);

                // If the annotation exists, capture its configured limit
                if (annotation != null) {
                    allowedLimit = annotation.limit();
                    redirect = annotation.redirect();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse HandlerMapping inside RateLimitFilter", e);
        }

        if (allowedLimit > 0) {
            String clientIp = httpRequest.getRemoteAddr();
            String requestUri = httpRequest.getRequestURI();

            // FIX: Create a unique compound key per IP + Endpoint combination
            String cacheKey = clientIp + ":" + requestUri;

            // Using your tracking map (Example assumes Caffeine Cache from previous step)
            AtomicInteger counter = requestCounts.get(cacheKey, k -> new AtomicInteger(0));
            int currentCount = counter.incrementAndGet();

            if (currentCount > allowedLimit) {
                resolver.resolveException(httpRequest, httpResponse, null, new RateLimitException("Too many requests. Limit is " + allowedLimit + " per minute", redirect));
                return;
            }
        }

        filterChain.doFilter(httpRequest, httpResponse);
    }
}
