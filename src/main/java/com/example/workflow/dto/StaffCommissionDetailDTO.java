package com.example.workflow.dto;

import com.example.workflow.nume.ConsultationAttributionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffCommissionDetailDTO {
    private Long attributionId;
    private Long staffId;
    private String staffName;
    private Long customerId;
    private String customerName;
    private Long orderId;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Long productVariantId;
    private String productVariantName;
    private Long consultationRequestId;
    private LocalDateTime consultationCreatedAt;
    private LocalDateTime consultationAcceptedAt;
    private LocalDateTime firstStaffReplyAt;
    private LocalDateTime orderCreatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private Integer orderedQuantity;
    private Integer receivedQuantity;
    private Double itemAmount;
    private Double bonusPercent;
    private Double bonusAmount;
    private ConsultationAttributionStatus status;
    private Boolean reviewed;
}
