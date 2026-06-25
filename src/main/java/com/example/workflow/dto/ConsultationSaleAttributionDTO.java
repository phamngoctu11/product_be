package com.example.workflow.dto;

import com.example.workflow.nume.ConsultationAttributionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSaleAttributionDTO implements Serializable {
    private Long id;
    private Long consultationRequestId;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long staffId;
    private String staffName;
    private Long productId;
    private String productName;
    private Long productVariantId;
    private String productVariantName;
    private LocalDateTime consultationCreatedAt;
    private LocalDateTime orderCreatedAt;
    private Long minutesFromConsultationToOrder;
    private Double itemAmount;
    private Boolean bonusEligible;
    private Double bonusPercent;
    private Double bonusAmount;
    private ConsultationAttributionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private Boolean reviewed;
}
