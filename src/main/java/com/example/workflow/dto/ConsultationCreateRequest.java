package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationCreateRequest {
    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be positive")
    private Long productId;

    @NotBlank(message = "First message is required")
    @Size(max = 2000, message = "First message must not exceed 2000 characters")
    private String firstMessage;
}
