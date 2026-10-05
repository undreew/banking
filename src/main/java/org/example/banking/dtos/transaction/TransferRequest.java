package org.example.banking.dtos.transaction;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {
    @NotNull(message = "Source Account ID required")
    private UUID sourceAccountId;

    @NotNull(message = "Destination Account ID required")
    private UUID destinationAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Amount format is invalid")
    private BigDecimal amount;

    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;
}
