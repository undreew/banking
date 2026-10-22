package org.example.banking.exceptions;

import lombok.Getter;
import org.example.banking.constants.errors.ErrorType;

@Getter
public abstract class ApiException extends RuntimeException {

    private final ErrorType type;
    private final Boolean redirect;

    protected ApiException(ErrorType type, String message, boolean redirect) {
        this(type, message, null, redirect);
    }

    protected ApiException(ErrorType type, String message, Throwable cause, boolean redirect) {
        super(message, cause);
        this.type = type;
        this.redirect = redirect;
    }
}
