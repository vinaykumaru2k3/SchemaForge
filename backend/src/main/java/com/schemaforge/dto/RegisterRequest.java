package com.schemaforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may only contain letters, digits, '_', '.' and '-'")
    private String username;

    @NotBlank(message = "Password is required")
    // Upper bound keeps us under bcrypt's 72-byte input limit for typical passwords.
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    private String password;
}
