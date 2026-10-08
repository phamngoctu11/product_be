package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CancelOrderRequest;
import com.example.workflow.dto.GuestOrderCancellationViewDTO;
import com.example.workflow.dto.OrderCancellationResultDTO;
import com.example.workflow.service.OrderCancellationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest/orders")
@RequiredArgsConstructor
@Validated
public class GuestOrderController {
    private final OrderCancellationService cancellationService;

    /** Safe link target: reading an email link never mutates the order. */
    @GetMapping("/{orderId}/cancellation")
    public ResponseEntity<ApiResponse<GuestOrderCancellationViewDTO>> cancellationView(
            @Positive @PathVariable Long orderId,
            @NotBlank @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(ApiResponse.success(cancellationService.getGuestCancellationView(orderId, token)));
    }

    @PostMapping("/{orderId}/cancellation")
    public ResponseEntity<ApiResponse<OrderCancellationResultDTO>> cancel(
            @Positive @PathVariable Long orderId,
            @NotBlank @RequestParam("token") String token,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        OrderCancellationResultDTO result = cancellationService.cancelByGuest(orderId, token, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Don hang da duoc huy.", result));
    }
}
