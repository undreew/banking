package org.example.banking.dtos.transaction;

import lombok.Data;
import org.example.banking.constants.transactions.TransactionStatus;
import org.example.banking.constants.transactions.TransactionType;
import org.example.banking.entities.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class TransactionResponse {
    private UUID transactionId;
//    private UUID sourceAccountId;
//    private UUID destinationAccountId;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private BigDecimal amount;
//    private Transaction reversalOfTransaction;
//    private Instant createdAt;
}
