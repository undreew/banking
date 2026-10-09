package org.example.banking.exceptions;

import org.example.banking.constants.errors.ErrorType;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(ErrorType.UNAUTHORIZED, message);
    }
}
