package com.example.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record SubmitOrderAgreementRequest(
        @NotNull Long orderVersion,
        @Size(max = 4000) String description,
        @Size(max = 10) List<@Size(max = 2048) String> imageUrls,
        @Min(1) Integer quantity,
        @Positive BigDecimal unitPrice,
        @Min(1) Integer productionDurationDays,
        Long userVoucherId,
        @Size(max = 1000) String note
) {
}
