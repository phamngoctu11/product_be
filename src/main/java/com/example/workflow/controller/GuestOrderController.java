package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CancelOrderRequest;
import com.example.workflow.dto.GuestOrderCancellationViewDTO;
import com.example.workflow.dto.OrderCancellationResultDTO;
import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderItemProductionDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.dto.ReceiptConfirmRequest;
import com.example.workflow.dto.ReceiptConfirmResponse;
import com.example.workflow.dto.ReceiptViewDTO;
import com.example.workflow.service.OrderCancellationService;
import com.example.workflow.service.OrderService;
import com.example.workflow.service.OrderDeliveryService;
import com.example.workflow.service.ProductionCheckpointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
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
    private final OrderService orderService;
    private final ProductionCheckpointService productionCheckpointService;
    private final OrderDeliveryService orderDeliveryService;

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
        return ResponseEntity.ok(ApiResponse.success("Đơn hàng đã được hủy.", result));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getGuestOrder(
            @Positive @PathVariable Long orderId,
            @NotBlank @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getGuestOrder(orderId, token)));
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryDTO>>> getGuestOrderHistory(
            @Positive @PathVariable Long orderId,
            @NotBlank @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getGuestOrderHistory(orderId, token)));
    }

    @GetMapping("/{orderId}/checkpoints")
    public ResponseEntity<ApiResponse<List<OrderItemProductionDTO>>> getGuestProductionCheckpoints(
            @Positive @PathVariable Long orderId,
            @NotBlank @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                productionCheckpointService.getGuestProgress(orderId, token)
        ));
    }

    @GetMapping("/{orderId}/receipt")
    public ResponseEntity<ApiResponse<ReceiptViewDTO>> getGuestReceiptView(
            @Positive @PathVariable Long orderId,
            @NotBlank @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderDeliveryService.getGuestReceiptView(orderId, token)));
    }

    @PostMapping("/{orderId}/receipt")
    public ResponseEntity<ApiResponse<ReceiptConfirmResponse>> confirmGuestReceipt(
            @Positive @PathVariable Long orderId,
            @NotBlank @RequestParam("token") String token,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody ReceiptConfirmRequest request
    ) {
        ReceiptConfirmResponse result = orderDeliveryService.confirmByGuest(orderId, token, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }
}
