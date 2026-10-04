package org.example.banking.dtos.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotBlank(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Amount format is invalid")
    private BigDecimal amount;

    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;
}
