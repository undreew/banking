package org.example.banking.dtos.user;

import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
public class UserUpdateStatusRequest {
    @NotNull(message = "Enabled is required")
    private Boolean enabled;

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;
}
