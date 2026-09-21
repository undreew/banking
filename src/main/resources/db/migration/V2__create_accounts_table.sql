CREATE TABLE accounts (
    id CHAR(36) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    user_id CHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_account_number (account_number),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_accounts_user_id ON accounts(user_id);