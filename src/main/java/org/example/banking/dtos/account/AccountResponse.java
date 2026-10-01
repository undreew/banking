package org.example.banking.dtos.account;

import org.example.banking.constants.accounts.AccountStatus;
import org.example.banking.constants.accounts.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private AccountType type;
    private BigDecimal balance;
    private AccountStatus status;
    private Instant createdAt;
}
