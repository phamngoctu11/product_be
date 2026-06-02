package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MomoPaymentRequest {
    @NotBlank(message = "Order id is required")
    private String orderId;

    @Positive(message = "Amount must be positive")
    private long amount;
}
