package org.example.banking.dtos.account;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import org.example.banking.constants.accounts.AccountType;

@Data
public class CreateAccountRequest {
    @NotNull(message = "Account type is required")
    private AccountType type;
}
