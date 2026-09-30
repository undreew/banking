package org.example.banking.dtos.user;

import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

}
