package org.example.banking.exceptions;

import lombok.Getter;
import org.example.banking.constants.errors.ErrorType;

@Getter
public abstract class ApiException extends RuntimeException {

    private final ErrorType type;

    protected ApiException(ErrorType type, String message) {
        this(type, message, null);
    }

    protected ApiException(ErrorType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }
}
