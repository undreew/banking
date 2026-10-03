package org.example.banking.exceptions;

import org.example.banking.constants.errors.ErrorType;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorType.NOT_FOUND, message);
    }
    
    public static NotFoundException of(String what, Object id) {
        return new NotFoundException(what + " " + id + " was not found");
    }
}
