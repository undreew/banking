package org.example.banking.dtos.transaction;

import org.example.banking.constants.transactions.TransactionStatus;
import org.example.banking.constants.transactions.TransactionType;

import java.time.Instant;
import java.util.UUID;

public record TransactionFilter(
        UUID id,
        TransactionType type,
        TransactionStatus status,
        Instant dateFrom,
        Instant dateTo
) {
}
