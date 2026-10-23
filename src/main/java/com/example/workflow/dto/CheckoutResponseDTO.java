package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO implements Serializable {
    private String status;
    private String message;
    private Long orderId;
    private Long version;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String paymentMethod;
    private String paymentStatus;
    private String voucherCode;
    private String voucherName;
    private String provider;
    private String url;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
    private String lookupToken;
    private String maskedEmail;
    private String guestWorkflowStatus;

}
