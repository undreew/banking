package org.example.banking.utils;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

public class ErrorUtils {

    public static String extractConstraintName(DataIntegrityViolationException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof ConstraintViolationException cve) {
            return cve.getConstraintName();
        }

        return null;
    }

    public static String mapConstraintToMessage(String constraintName) {
        if (constraintName == null) {
            return "A data conflict occurred. Please check your input.";
        }

        return switch (constraintName.toLowerCase()) {
            case "account_number" ->  "Account number cannot be null.";
            case "users.uk_users_email" -> "This email is already registered.";
            case "users.uk_accounts_account_number" -> "This account number already exists.";
            case "transactions.uk_transactions_idempotency_key" -> "This transaction already exists.";
            default -> "A record with these details already exists.";
        };
    }
}
