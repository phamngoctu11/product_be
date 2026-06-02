package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.service.StaffOrderService;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders/staff")
@RequiredArgsConstructor
@Validated
public class StaffOrderController {
    private final StaffOrderService staffOrderService;

    @GetMapping("/warehouse-pending")
    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getWarehousePendingOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(staffOrderService.getWarehousePendingOrders(pageable)));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getMyAssignedStaffOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(staffOrderService.getMyAssignedStaffOrders(pageable)));
    }

    @PostMapping("/claim/{orderId}")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Void>> claimWarehouseOrder(
            @Positive @PathVariable Long orderId
    ) {
        try {
            staffOrderService.claimWarehouseOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success("Nhan phu trach don hang thanh cong."));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/export/{orderId}")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Void>> exportOrder(
            @Positive @PathVariable Long orderId,
            @RequestBody List<ItemCheckRequest> exportData
    ) {
        try {
            staffOrderService.exportOrder(orderId, exportData);
            return ResponseEntity.ok(ApiResponse.success("Ghi nhan xuat kho thanh cong, dang cho quan ly KCS."));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
