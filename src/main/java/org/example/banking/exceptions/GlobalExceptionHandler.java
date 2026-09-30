package org.example.banking.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.banking.utils.ErrorUtils;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.example.banking.constants.errors.ErrorType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.time.Instant;
import java.util.HashMap;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(MethodArgumentNotValidException exception) {
        var errors = new HashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationError(DataIntegrityViolationException exception) {
        String constraintName = ErrorUtils.extractConstraintName(exception);
        String message = ErrorUtils.mapConstraintToMessage(constraintName);

        logger.warn("constraint violation : {}", exception.getMostSpecificCause().getMessage());
        return respond(ErrorType.CONFLICT, message, null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException exception) {
        return respond(ErrorType.NOT_FOUND, exception.getMessage(), null);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorType type, String detail, Map<String, String> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.getStatus(), detail);
        problem.setProperty("timestamp", Instant.now().toString());
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(type.getStatus()).body(problem);
    }
}
