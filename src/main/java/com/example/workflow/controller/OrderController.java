package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CancelOrderRequest;
import com.example.workflow.dto.OrderCancellationResultDTO;
import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.dto.ReceiptConfirmRequest;
import com.example.workflow.dto.ReceiptConfirmResponse;
import com.example.workflow.dto.ReceiptViewDTO;
import com.example.workflow.dto.ReorderResponseDTO;
import com.example.workflow.dto.OrderItemProductionDTO;
import com.example.workflow.dto.ChangeRequestDTO;
import com.example.workflow.dto.CreateChangeRequestRequest;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.OrderService;
import com.example.workflow.service.OrderCancellationService;
import com.example.workflow.service.ProductionCheckpointService;
import com.example.workflow.service.ChangeRequestService;
import com.example.workflow.service.OrderDeliveryService;
import com.example.workflow.service.OrderReorderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;
    private final OrderCancellationService orderCancellationService;
    private final ProductionCheckpointService productionCheckpointService;
    private final ChangeRequestService changeRequestService;
    private final OrderDeliveryService orderDeliveryService;
    private final OrderReorderService orderReorderService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getMyOrders(
            @DecimalMin(value = "0.0", message = "Minimum price must not be negative")
            @RequestParam(required = false) BigDecimal minPrice,
            @DecimalMin(value = "0.0", message = "Maximum price must not be negative")
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        validatePriceRange(minPrice, maxPrice);
        Page<OrderListDTO> rs = orderService.getMyOrders(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.success(rs));
    }


    @GetMapping("/me/cancelled")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getMyCancelledOrders(
            @DecimalMin(value = "0.0", message = "Minimum price must not be negative")
            @RequestParam(required = false) BigDecimal minPrice,
            @DecimalMin(value = "0.0", message = "Maximum price must not be negative")
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        validatePriceRange(minPrice, maxPrice);
        Page<OrderListDTO> rs = orderService.getMyCancelledOrders(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.success(rs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@Positive @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @GetMapping("/{order_id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryDTO>>> getOrderHistory(
            @Positive @PathVariable("order_id") Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderHistory(orderId)));
    }

    @GetMapping("/{orderId}/checkpoints")
    @PreAuthorize("hasAnyAuthority('USER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<OrderItemProductionDTO>>> getProductionCheckpoints(
            @Positive @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(productionCheckpointService.getProgress(orderId)));
    }

    @GetMapping("/{orderId}/change-requests")
    @PreAuthorize("hasAnyAuthority('USER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ChangeRequestDTO>>> getChangeRequests(
            @Positive @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(changeRequestService.list(orderId)));
    }

    @PostMapping("/{orderId}/change-requests")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ChangeRequestDTO>> createChangeRequest(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody CreateChangeRequestRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Yêu cầu chỉnh sửa đã được gửi.",
                changeRequestService.create(orderId, request, idempotencyKey)
        ));
    }

    @PostMapping("/{order_id}/cancellation")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<OrderCancellationResultDTO>> cancelOrder(
            @Positive @PathVariable("order_id") Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        OrderCancellationResultDTO result = orderCancellationService.cancelByCurrentUser(orderId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Ban da huy don hang thanh cong.", result));
    }

    @PostMapping("/{order_id}/reorder")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ReorderResponseDTO>> reorderOrder(
            @Positive @PathVariable("order_id") Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey
    ) {
        ReorderResponseDTO response = orderReorderService.reorder(orderId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Reorder processed", response));
    }

    @GetMapping("/{orderId}/receipt")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ReceiptViewDTO>> getReceiptView(
            @Positive @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderDeliveryService.getUserReceiptView(orderId)));
    }

    @PostMapping("/{orderId}/receipt")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ReceiptConfirmResponse>> confirmReceipt(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody ReceiptConfirmRequest request
    ) {
        ReceiptConfirmResponse response = orderDeliveryService.confirmByUser(orderId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Minimum price must not be greater than maximum price."
            );
        }
    }
}
