package org.example.banking.dtos.account;

import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.example.banking.constants.accounts.AccountStatus;

@Data
public class UpdateAccountStatusRequest {
    @NotNull(message = "Account status is required")
    private AccountStatus status;
    
    @NotBlank(message = "Reason must not be blank")
    @Size(max = 500)
    private String reason;
}
