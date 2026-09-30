package org.example.banking.constants.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    CONFLICT("conflict", HttpStatus.CONFLICT),
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND);

    private final String slug;
    private final HttpStatus status;

    ErrorType(String slug, HttpStatus status) {
        this.slug = slug;
        this.status = status;
    }
}
