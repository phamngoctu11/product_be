package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CheckoutRequest;
import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<CheckoutResponseDTO>> checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request
    ) {
        CheckoutResponseDTO response = checkoutService.checkoutCurrentUser(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
