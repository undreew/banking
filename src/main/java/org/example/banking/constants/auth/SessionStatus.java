package org.example.banking.constants.auth;

/** * Lifecycle of one row in {@code refresh_tokens}. 
 * * ACTIVE: its refresh value can be presented. 
 * * ROTATED: superseded by {@code replaced_by}; the user's session continues in that row.
 * * REVOKED: ended by logout, reuse detection, a session cap or an administrator. 
 * */
public enum SessionStatus {
    ACTIVE,
    ROTATED,
    REVOKED
}
