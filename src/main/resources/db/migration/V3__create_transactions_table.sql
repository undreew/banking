CREATE TABLE transactions (
    id CHAR(36) NOT NULL,
    source_account_id CHAR(36) NULL,
    destination_account_id CHAR(36) NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) NULL,
    reversal_of_transaction_id CHAR(36) NULL,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_idempotency_key (idempotency_key),
    CONSTRAINT fk_transactions_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transactions_destination_account FOREIGN KEY (destination_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transactions_reversal_of FOREIGN KEY (reversal_of_transaction_id) REFERENCES transactions(id),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_transactions_source_account ON transactions(source_account_id);
CREATE INDEX idx_transactions_destination_account ON transactions(destination_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_status ON transactions(status);