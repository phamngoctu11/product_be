package com.example.workflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GuestCheckoutRequest {
    @NotBlank(message = "Customer name is required")
    @Size(max = 120, message = "Customer name must be at most 120 characters")
    private String customerName;

    @NotBlank(message = "Phone is required")
    @Size(max = 30, message = "Phone must be at most 30 characters")
    @Pattern(regexp = "^[0-9+().\\-\\s]{8,30}$", message = "Phone is invalid")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must be at most 500 characters")
    private String shippingAddress;

    @Size(max = 1000, message = "Note must be at most 1000 characters")
    private String note;

    @NotEmpty(message = "Select at least one variant to checkout")
    private List<@Positive(message = "Variant id must be positive") Long> variantIds;
}
