package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public VoucherTemplateDTO(String code,String name,String description,
                              int pointCost,double minOrderValue,
                              double discountPercent,
                              boolean isActive)
    {
        this.code = code;
        this.name = name;
        this.minOrderValue = minOrderValue;
        this.description =description;
        this.pointCost = pointCost;
        this.discountPercent = discountPercent;
        this.isActive = isActive;
    }
}