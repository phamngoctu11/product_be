package com.example.workflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SubmitCustomRequest(
        @NotNull(message = "Expected version is required")
        @PositiveOrZero(message = "Expected version must not be negative") Long expectedVersion,
        @Size(max = 120, message = "Full name must be at most 120 characters") String fullName,
        @Email(message = "Email is invalid")
        @Size(max = 255, message = "Email must be at most 255 characters") String email,
        @Size(max = 30, message = "Phone must be at most 30 characters") String phone,
        String address,
        @Size(max = 1000, message = "Note must be at most 1000 characters") String note
) {
}
