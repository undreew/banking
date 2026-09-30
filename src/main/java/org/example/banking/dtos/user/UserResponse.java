package org.example.banking.dtos.user;

import lombok.Data;
import org.example.banking.constants.accounts.Role;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private boolean enabled;
    private Instant createdAt;
}
