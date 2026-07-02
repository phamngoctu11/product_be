package com.example.workflow.controller;

import com.example.workflow.dto.AdminReviewRequest;
import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.ManagerOrderService;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class ManagerOrderController {
    private final ManagerOrderService managerOrderService;

    @PostMapping("/admin/pending")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderListDTO>>> getPendingOrders(
            @RequestParam OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(managerOrderService.getPendingOrders(status,pageable)));
    }

    @PostMapping("/manager/review-order/{orderId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> reviewOrder(
            @Positive @PathVariable Long orderId,
            @Valid @RequestBody AdminReviewRequest request,
            @RequestParam("changerId") String changerId,
            @RequestParam(value = "staffId", required = false) String staffId
    ) {
        try {
            managerOrderService.reviewOrder(orderId, request, changerId, staffId);
            return ResponseEntity.ok(ApiResponse.success("Duyet don thanh cong!"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }

    @PostMapping("/manager/assign-staff/{orderId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> assignStaffToOrder(
            @Positive @PathVariable Long orderId,
            @RequestParam("staffId") String staffId
    ) {
        try {
            managerOrderService.assignStaffToOrder(orderId, staffId);
            return ResponseEntity.ok(ApiResponse.success("Gan nhan vien phu trach don hang thanh cong."));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }

    @PostMapping("/manager/kcs-check/{orderId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> kcsCheck(
            @Positive @PathVariable Long orderId,
            @RequestParam("isPassed") boolean isPassed,
            @Nullable @RequestParam("cancelReason") String cancelReason
    ) {
        try {
            managerOrderService.kcsCheck(orderId, isPassed,cancelReason);
            return ResponseEntity.ok(ApiResponse.success("KCS hoan tat!"));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }
}
