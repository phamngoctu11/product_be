package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.OrderAgreementDTO;
import com.example.workflow.dto.OrderAgreementDecisionRequest;
import com.example.workflow.dto.SubmitOrderAgreementRequest;
import com.example.workflow.service.OrderAgreementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderAgreementController {
    private final OrderAgreementService agreementService;

    @GetMapping("/{orderId}/agreement")
    @PreAuthorize("hasAnyAuthority('USER','STAFF')")
    public ResponseEntity<ApiResponse<OrderAgreementDTO>> getAgreement(@Positive @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(agreementService.getForCurrentParticipant(orderId)));
    }

    @PostMapping("/{orderId}/agreement/submissions")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<OrderAgreementDTO>> submit(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody SubmitOrderAgreementRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã gửi thỏa thuận cho nhân viên xác nhận.",
                agreementService.submit(orderId, request, idempotencyKey)
        ));
    }

    @PostMapping("/staff/{orderId}/agreement/decisions")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<OrderAgreementDTO>> decide(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody OrderAgreementDecisionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã ghi nhận quyết định thỏa thuận.",
                agreementService.decide(orderId, request, idempotencyKey)
        ));
    }
}
