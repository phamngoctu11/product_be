package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

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
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String unavailableReason;
}
