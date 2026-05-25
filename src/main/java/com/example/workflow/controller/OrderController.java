package com.example.workflow.controller;

import com.example.workflow.dto.AdminReviewRequest;
import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping("/user/{user_id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderListDTO>>> getAllMyOrders(
            @Positive @PathVariable("user_id") Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByUserId(userId)));
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @Positive @PathVariable("order_id") Long orderId,
            @NotBlank @Size(max = 500) @RequestParam("reason") String reason
    ) {
        try {
            orderService.cancelOrder(orderId, reason);
            return ResponseEntity.ok(ApiResponse.success("Ban da huy don hang thanh cong."));
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/customer/confirm-receipt/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> confirmReceipt(
            @Positive @PathVariable Long orderId,
            @RequestBody List<ItemCheckRequest> receiptData
    ) {
        try {
            orderService.confirmCustomerReceipt(orderId, receiptData);
            return ResponseEntity.ok(ApiResponse.success("Xac nhan nhan hang thanh cong. Cam on ban!"));
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderListDTO>>> getPendingOrders() {
        List<OrderListDTO> pendingOrders = orderRepository.findListDtoByStatus(OrderStatus.PENDING_APPROVAL);
        return ResponseEntity.ok(ApiResponse.success(pendingOrders));
    }

    @PostMapping("/manager/review-order/{orderId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> reviewOrder(
            @Positive @PathVariable Long orderId,
            @Valid @RequestBody AdminReviewRequest request
    ) {
        try {
            orderService.processAdminReview(orderId, request);
            return ResponseEntity.ok(ApiResponse.success("Duyet don thanh cong!"));
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/manager/kcs-check/{orderId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> kcsCheck(
            @Positive @PathVariable Long orderId,
            @RequestParam("isPassed") boolean isPassed
    ) {
        try {
            orderService.processManagerKcsCheck(orderId, isPassed);
            return ResponseEntity.ok(ApiResponse.success("KCS hoan tat!"));
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/staff/export/{orderId}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<Void>> exportOrder(
            @Positive @PathVariable Long orderId,
            @RequestBody List<ItemCheckRequest> exportData
    ) {
        try {
            orderService.processStaffExport(orderId, exportData);
            return ResponseEntity.ok(ApiResponse.success("Ghi nhan xuat kho thanh cong, dang cho quan ly KCS."));
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
