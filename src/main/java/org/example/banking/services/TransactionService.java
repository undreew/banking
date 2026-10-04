package org.example.banking.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.dtos.transaction.DepositRequest;
import org.example.banking.dtos.transaction.WithdrawRequest;
import org.example.banking.mappers.TransactionMapper;
import org.example.banking.repositories.AccountRepository;
import org.example.banking.repositories.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountRepository accountRepository;

    public void getAllTransactions() {
    }

    public void reverseTransaction() {
    }

    public void depositTransaction(UUID id, DepositRequest request) {
    }

    public void withdrawTransaction(UUID id, @Valid WithdrawRequest request) {
    }

    private void checkAccountExists(UUID id) {
    }
}
