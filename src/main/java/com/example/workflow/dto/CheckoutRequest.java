package com.example.workflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    @NotEmpty(message = "Select at least one variant to checkout")
    private List<@Positive(message = "Variant id must be positive") Long> variantIds;

    @Positive(message = "User voucher id must be positive")
    private Long userVoucherId;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "(?i)COD|ONLINE", message = "Payment method must be COD or ONLINE")
    private String paymentMethod;

    @Size(max = 1000, message = "Note must be at most 1000 characters")
    private String note;
}
