package org.example.banking.exceptions;

import org.example.banking.constants.errors.ErrorType;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(ErrorType.CONFLICT, message, false);
    }
}
