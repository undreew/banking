package org.example.banking.dtos.account;

import lombok.Data;
import lombok.AllArgsConstructor;
import org.example.banking.constants.accounts.AccountType;
import org.example.banking.constants.accounts.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private AccountType type;
    private BigDecimal balance;
    private AccountStatus status;
    private Instant createdAt;
}
