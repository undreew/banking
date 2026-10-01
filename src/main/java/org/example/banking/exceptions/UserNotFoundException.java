package org.example.banking.exceptions;

import org.example.banking.constants.errors.ErrorType;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException() {
        super(ErrorType.NOT_FOUND, "User not found.");
    }
    public UserNotFoundException(String message) {
        super(ErrorType.NOT_FOUND, message);
    }
}
