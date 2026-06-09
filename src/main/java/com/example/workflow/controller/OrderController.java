package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.dto.ReceiptComplaintRequest;
import com.example.workflow.dto.ReceiptConfirmRequest;
import com.example.workflow.dto.ReceiptConfirmResponse;
import com.example.workflow.exception.AppException;
import com.example.workflow.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/user/{user_id}")
    @PreAuthorize("hasAnyAuthority('USER', 'MANAGER', 'STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getAllMyOrders(
            @Positive @PathVariable("user_id") Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<OrderListDTO> rs = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(rs));
    }


    @GetMapping("/user/{user_id}/cancelled")
    @PreAuthorize("hasAnyAuthority('USER', 'MANAGER','STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getAllMyCancelledOrders(
            @Positive @PathVariable("user_id") Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<OrderListDTO> rs = orderService.getCancelledOrdersByUserId(userId, pageable);
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

    @PutMapping("/{order_id}/cancel")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @Positive @PathVariable("order_id") Long orderId,
            @NotBlank @Size(max = 500) @RequestParam("reason") String reason
    ) {
        try {
            orderService.cancelOrder(orderId, reason);
            return ResponseEntity.ok(ApiResponse.success("Ban da huy don hang thanh cong."));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/customer/confirm-receipt/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReceiptConfirmResponse>> confirmReceipt(
            @Positive @PathVariable Long orderId,
            @Valid @RequestBody ReceiptConfirmRequest request
    ) {
        try {
            ReceiptConfirmResponse response = orderService.confirmCustomerReceipt(orderId, request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/customer/receipt-complaint/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReceiptConfirmResponse>> sendReceiptComplaint(
            @Positive @PathVariable Long orderId,
            @Valid @RequestBody ReceiptComplaintRequest request
    ) {
        try {
            ReceiptConfirmResponse response = orderService.sendReceiptComplaint(orderId, request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
