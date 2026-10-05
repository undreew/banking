package org.example.banking.dtos.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReverseTransactionRequest {
    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Description must be under 500 characters")
    private String reason;
}
