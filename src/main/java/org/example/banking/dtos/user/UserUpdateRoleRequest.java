package org.example.banking.dtos.user;

import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.example.banking.constants.accounts.Role;

@Data
public class UserUpdateRoleRequest {
    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;
}
