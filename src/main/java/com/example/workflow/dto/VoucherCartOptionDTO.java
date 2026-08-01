package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherCartOptionDTO {
    private Long userVoucherId;
    private Long templateId;
    private String source;
    private VoucherTemplateDTO template;
    private boolean applicable;
    private boolean best;
    private double discountAmount;
    private double finalPrice;
    private String unavailableReason;
}
