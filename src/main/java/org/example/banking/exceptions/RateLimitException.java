package org.example.banking.exceptions;

import org.example.banking.constants.errors.ErrorType;

public class RateLimitException extends ApiException {
    public RateLimitException(String message) {
        super(ErrorType.RATE_LIMITED, message);
    }
}
