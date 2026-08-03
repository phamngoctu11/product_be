package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Username or email is required")
        @Size(max = 255, message = "Username or email must be at most 255 characters")
        String identifier
) {
}
