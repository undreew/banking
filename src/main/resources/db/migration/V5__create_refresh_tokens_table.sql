-- V5__create_refresh_tokens_table.sql
CREATE TABLE refresh_tokens
(
    id          BINARY(16)   NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    expires_at  TIMESTAMP(6) NOT NULL,
    revoked_at  TIMESTAMP(6) NULL     DEFAULT NULL,
    replaced_by BINARY(16)   NULL     DEFAULT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT refresh_tokens_pk PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE RESTRICT,

    KEY refresh_tokens_user_revoked_idx (user_id, revoked_at),
    KEY refresh_tokens_expires_idx (expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;