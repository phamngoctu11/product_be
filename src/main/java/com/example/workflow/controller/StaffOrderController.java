package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ClaimOrderRequest;
import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderAssignmentResultDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.OrderService;
import com.example.workflow.service.OrderAssignmentService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders/staff")
@RequiredArgsConstructor
@Validated
public class StaffOrderController {
    private final OrderService orderService;
    private final OrderAssignmentService assignmentService;

    @GetMapping("/claimable")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getClaimableOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.listClaimable(pageable)));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getMyAssignedStaffOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyAssignedStaffOrders(pageable)));
    }

    @PostMapping("/claim/{orderId}")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<OrderAssignmentResultDTO>> claimOrder(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody ClaimOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Nhận phụ trách đơn hàng thành công.",
                assignmentService.claim(orderId, request, idempotencyKey)
        ));
    }

    @PostMapping("/export/{orderId}")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Void>> exportOrder(
            @Positive @PathVariable Long orderId,
            @RequestBody List<ItemCheckRequest> exportData
    ) {
        try {
            orderService.processStaffExport(orderId, exportData);
            return ResponseEntity.ok(ApiResponse.success("Ghi nhan xuat kho thanh cong, dang cho quan ly KCS."));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }
}
