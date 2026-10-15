package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.PaymentAttemptDTO;
import com.example.workflow.service.PaymentService;
import com.example.workflow.service.PaymentWebhookService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentWebhookService webhookService;

    @PostMapping("/api/payments/orders/{orderId}/attempts")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<PaymentAttemptDTO>> openPayment(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment attempt opened.",
                paymentService.openPayment(orderId, idempotencyKey)
        ));
    }

    @GetMapping("/api/payments/orders/{orderId}/status")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<PaymentAttemptDTO>> paymentStatus(
            @Positive @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentStatus(orderId)));
    }

    @PostMapping(
            value = "/api/webhooks/sepay",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Boolean>> sePayWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader HttpHeaders headers
    ) {
        webhookService.process(rawBody, headers);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
