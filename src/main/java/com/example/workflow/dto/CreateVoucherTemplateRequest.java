package com.example.workflow.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CreateVoucherTemplateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @Min(0) int pointCost,
        @PositiveOrZero double minOrderValue,
        @PositiveOrZero double discountPercent,
        @PositiveOrZero double maxDiscountAmount,
        @Min(0) int quantity,
        boolean guestVoucher,
        @NotNull @Future LocalDateTime expiryDate
) implements Serializable {
}
