package org.example.banking.dtos.account;

import org.example.banking.constants.accounts.AccountType;

import java.math.BigDecimal;

public class AccountSummaryResponse {
    private String accountNumber;
    private AccountType type;
    private BigDecimal balance;
}
