package org.example.banking.constants.audit;

public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    ACCOUNT_LOCKED,
    TOKEN_REFRESHED,
    REFRESH_REUSE_DETECTED,
    LOGOUT,
    LOGOUT_ALL,
    SESSION_REVOKED,
    ROLE_CHANGED,
    STATUS_CHANGED
}
