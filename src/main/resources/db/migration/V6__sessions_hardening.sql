-- V6__sessions_hardening.sql
-- Sessions: opaque hashed refresh tokens, explicit status, absolute expiry, device metadata.
-- Users: lockout bookkeeping. Audit: client IP.

ALTER TABLE refresh_tokens
    ADD COLUMN token_hash CHAR(64) NULL AFTER id,
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER user_id,
    ADD COLUMN absolute_expires_at TIMESTAMP(6) NULL AFTER expires_at,
    ADD COLUMN user_agent VARCHAR(255) NULL,
    ADD COLUMN ip_address VARCHAR(45) NULL,
    ADD COLUMN last_used_at TIMESTAMP(6) NULL;

-- Rows issued before this migration hold signed-JWT refresh tokens whose hash is unknown.
-- They cannot be presented against the new scheme, so they are closed. Users log in again.
UPDATE refresh_tokens
SET status = CASE WHEN replaced_by IS NOT NULL THEN 'ROTATED' ELSE 'REVOKED' END,
    revoked_at = COALESCE(revoked_at, CURRENT_TIMESTAMP(6))
WHERE token_hash IS NULL;

-- Unique placeholder hash (64 hex chars) so the NOT NULL + UNIQUE constraints can be added.
UPDATE refresh_tokens
SET token_hash = SHA2(LOWER(HEX(id)), 256)
WHERE token_hash IS NULL;

UPDATE refresh_tokens
SET absolute_expires_at = expires_at
WHERE absolute_expires_at IS NULL;

ALTER TABLE refresh_tokens
    MODIFY COLUMN token_hash CHAR(64) NOT NULL,
    MODIFY COLUMN absolute_expires_at TIMESTAMP(6) NOT NULL,
    ADD UNIQUE KEY refresh_tokens_hash_uk (token_hash),
    ADD KEY refresh_tokens_user_status_idx (user_id, status);

ALTER TABLE users
    ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0,
    ADD COLUMN failed_login_window_start TIMESTAMP(6) NULL,
    ADD COLUMN locked_until TIMESTAMP(6) NULL;

ALTER TABLE audit_logs
    ADD COLUMN ip_address VARCHAR(45) NULL;