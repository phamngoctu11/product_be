package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherTemplateDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private int pointCost;
    private double minOrderValue;
    private double discountPercent;
    private double maxDiscountAmount;
    private int quantity;
    private boolean isActive;
    private LocalDateTime expiryDate;
}