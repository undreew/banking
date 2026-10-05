package org.example.banking.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.banking.utils.ErrorUtils;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import jakarta.validation.UnexpectedTypeException;
import org.example.banking.constants.errors.ErrorType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import tools.jackson.databind.exc.InvalidFormatException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;
import java.util.Arrays;
import java.time.Instant;
import java.util.HashMap;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // handler of all ApiException type
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> onApiException(ApiException ex) {
        // 4xx is the caller's problem and needs no stack trace; 5xx is ours and needs one.
        if (ex.getType().getStatus().is5xxServerError()) {
            logger.error("{} : {}", ex.getType().getSlug(), ex.getMessage(), ex);
        } else {
            logger.info("{} : {}", ex.getType().getSlug(), ex.getMessage());
        }
        return respond(ex.getType(), ex.getMessage(), null);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationError(MethodArgumentNotValidException exception) {
        var errors = new HashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return respond(ErrorType.VALIDATION_FAILED, "One or more fields are invalid", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationError(DataIntegrityViolationException exception) {
        String constraintName = ErrorUtils.extractConstraintName(exception);
        String message = ErrorUtils.mapConstraintToMessage(constraintName);

        logger.warn("constraint violation : {}", exception.getMostSpecificCause().getMessage());
        return respond(ErrorType.CONFLICT, message, null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException exception) {
        return respond(ErrorType.NOT_FOUND, exception.getMessage(), null);
    }
    
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleObjectOptimisticLockingFailure(ObjectOptimisticLockingFailureException exception) {
        return respond(ErrorType.CONFLICT, "This resource was updated by another request. Please try again.", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("JSON parse error or malformed request payload", ex);

        String detailMessage = "The request body contains invalid or malformed data.";
        Map<String, String> fieldErrors = null;

        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof InvalidFormatException ife) {
            fieldErrors = new HashMap<>();

            // Extract the field name dynamically (e.g., "role")
            String fieldName = ife.getPath().isEmpty() ? "field" : ife.getPath().get(0).getPropertyName();

            if (ife.getTargetType().isEnum()) {
                // Dynamically fetch the accepted values: [USER, ADMIN]
                String acceptedValues = Arrays.toString(ife.getTargetType().getEnumConstants());
                String errorMessage = String.format("Invalid value '%s'. Accepted values are: %s", ife.getValue(), acceptedValues);

                fieldErrors.put(fieldName, errorMessage);
                detailMessage = String.format("Invalid enum value for field '%s'.", fieldName);
            } else {
                String errorMessage = String.format("Invalid value '%s'. Expected data type: %s", ife.getValue(), ife.getTargetType().getSimpleName());

                fieldErrors.put(fieldName, errorMessage);
                detailMessage = String.format("Data type mismatch for field '%s'.", fieldName);
            }
        }
        return respond(ErrorType.MALFORMED_REQUEST, detailMessage, fieldErrors);
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedType(UnexpectedTypeException ex) {
        logger.warn("unhandled exception", ex.getMessage());
        return respond(ErrorType.INTERNAL, "An unexpected error occurred", null);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorType type, String detail, Map<String, String> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.getStatus(), detail);
        problem.setTitle(type.getSlug());
        problem.setProperty("timestamp", Instant.now().toString());
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(type.getStatus()).body(problem);
    }
}
