package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO implements Serializable {
    private String status;
    private String message;
    private Long orderId;
    private Double totalPrice;
    private Double discountAmount;
    private Double finalPrice;
    private String paymentMethod;
    private String voucherCode;
    private String voucherName;
    private String provider;
    private String url;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;

    public static CheckoutResponseDTO fromMap(Map<String, String> response) {
        CheckoutResponseDTO dto = new CheckoutResponseDTO();
        if (response == null) {
            return dto;
        }
        dto.setStatus(response.get("status"));
        dto.setMessage(response.get("message"));
        dto.setOrderId(parseLong(response.get("orderId")));
        dto.setTotalPrice(parseDouble(response.get("totalPrice")));
        dto.setDiscountAmount(parseDouble(response.get("discountAmount")));
        dto.setFinalPrice(parseDouble(response.get("finalPrice")));
        dto.setPaymentMethod(response.get("paymentMethod"));
        dto.setVoucherCode(response.get("voucherCode"));
        dto.setVoucherName(response.get("voucherName"));
        dto.setProvider(response.get("provider"));
        dto.setUrl(response.get("url"));
        dto.setPayUrl(response.get("payUrl"));
        dto.setDeeplink(response.get("deeplink"));
        dto.setQrCodeUrl(response.get("qrCodeUrl"));
        return dto;
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.valueOf(value);
    }
}
