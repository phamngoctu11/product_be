package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherTemplateDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private int pointCost;
    private BigDecimal minOrderValue;
    private BigDecimal discountPercent;
    private BigDecimal maxDiscountAmount;
    private int quantity;
    private boolean isActive;
    private boolean guestVoucher;
    private LocalDateTime expiryDate;
}
