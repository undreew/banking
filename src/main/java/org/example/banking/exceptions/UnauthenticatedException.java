package org.example.banking.exceptions;

import org.example.banking.constants.errors.ErrorType;

public class UnauthenticatedException extends ApiException {

    private static final String UNIFORM_MESSAGE = "Invalid email or password";

    public UnauthenticatedException() {
        super(ErrorType.UNAUTHENTICATED, UNIFORM_MESSAGE);
    }

    public UnauthenticatedException(String message) {
        super(ErrorType.UNAUTHENTICATED, message);
    }

    public static UnauthenticatedException sessionNotValid() {
        return new UnauthenticatedException("Session is no longer valid");
    }
}
