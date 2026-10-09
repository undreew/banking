package org.example.banking.dtos.auth;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.banking.constants.accounts.Role;

import org.example.banking.validators.Lowercase;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Lowercase(message = "Email must be in lowercase")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")
    @Size(min = 6, max = 25, message = "Password must be between 6 to 25 characters long")
    private String password;

    @NotBlank(message = "First Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String firstName;

    @NotBlank(message = "Last Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String lastName;
}
