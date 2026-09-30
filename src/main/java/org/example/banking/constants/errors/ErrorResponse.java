package org.example.banking.constants.errors;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details
) {
    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path, null);
    }
}
