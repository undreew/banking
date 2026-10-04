package org.example.banking.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.banking.constants.transactions.TransactionStatus;
import org.example.banking.constants.transactions.TransactionType;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction {
    @Id
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    @Column(name = "amount")
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    @Column(name = "idempotency_key")
    private String idempotencyKey; // string for now
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_transaction_id")
    private Transaction reversalOfTransaction;
    
    @Column(name = "description")
    private String description;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
