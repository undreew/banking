package org.example.banking.constants.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    CONFLICT("conflict", HttpStatus.CONFLICT),
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("unauthorized", HttpStatus.UNAUTHORIZED),
    INTERNAL("internal", HttpStatus.INTERNAL_SERVER_ERROR),
    RATE_LIMITED("rate-limited", HttpStatus.TOO_MANY_REQUESTS),
    UNAUTHENTICATED("unauthenticated", HttpStatus.UNAUTHORIZED),
    MALFORMED_REQUEST("malformed-request", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("validation-failed", HttpStatus.BAD_REQUEST);

    private final String slug;
    private final HttpStatus status;

    ErrorType(String slug, HttpStatus status) {
        this.slug = slug;
        this.status = status;
    }
}
