package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.GuestCheckoutRequest;
import com.example.workflow.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest-checkout")
@RequiredArgsConstructor
@Validated
public class GuestCheckoutController {
    private static final String GUEST_SESSION_HEADER = "X-Guest-Session-Id";

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponseDTO>> checkout(
            @RequestHeader(GUEST_SESSION_HEADER) String guestSessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody GuestCheckoutRequest request
    ) {
        CheckoutResponseDTO response = cartService.checkoutGuestCart(
                guestSessionId,
                request,
                idempotencyKey
        );
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
