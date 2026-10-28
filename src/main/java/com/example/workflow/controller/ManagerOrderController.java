package com.example.workflow.controller;

import com.example.workflow.dto.AssignOrderRequest;
import com.example.workflow.dto.AvailableStaffDTO;
import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ManagerOrderReviewDetailDTO;
import com.example.workflow.dto.ManagerOrderReviewSummaryDTO;
import com.example.workflow.dto.ManagerReviewRequest;
import com.example.workflow.dto.ManagerReviewResultDTO;
import com.example.workflow.dto.OrderAssignmentResultDTO;
import com.example.workflow.dto.OrderAssignmentQueueDTO;
import com.example.workflow.dto.ReviewProductionCheckpointRequest;
import com.example.workflow.dto.ReviewProductionCheckpointResultDTO;
import com.example.workflow.service.ManagerOrderReviewService;
import com.example.workflow.service.OrderAssignmentService;
import com.example.workflow.service.ProductionCheckpointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class ManagerOrderController {
    private final ManagerOrderReviewService reviewService;
    private final OrderAssignmentService assignmentService;
    private final ProductionCheckpointService productionCheckpointService;

    @GetMapping("/manager/reviews")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<Page<ManagerOrderReviewSummaryDTO>>> getPendingReviews(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.listPending(pageable)));
    }

    @GetMapping("/manager/reviews/{orderId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<ManagerOrderReviewDetailDTO>> getReviewDetail(
            @Positive @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewDetail(orderId)));
    }

    @PostMapping("/manager/reviews/{orderId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<ManagerReviewResultDTO>> reviewOrder(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody ManagerReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã ghi nhận quyết định của quản lý.",
                reviewService.review(orderId, request, idempotencyKey)
        ));
    }

    @GetMapping("/manager/available-staff")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<Page<AvailableStaffDTO>>> getAvailableStaff(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.listAvailableStaff(pageable)));
    }

    @GetMapping("/manager/assignments")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderAssignmentQueueDTO>>> getAssignableOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.listAssignable(pageable)));
    }

    @PostMapping("/manager/assignments/{orderId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<OrderAssignmentResultDTO>> assignStaffToOrder(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody AssignOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Gán nhân viên phụ trách đơn hàng thành công.",
                assignmentService.assignByManager(orderId, request, idempotencyKey)
        ));
    }

    @PostMapping("/manager/{orderId}/checkpoints/{checkpointId}/review")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<ApiResponse<ReviewProductionCheckpointResultDTO>> reviewProductionCheckpoint(
            @Positive @PathVariable Long orderId,
            @Positive @PathVariable Long checkpointId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody ReviewProductionCheckpointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã ghi nhận quyết định kiểm tra sản xuất.",
                productionCheckpointService.review(orderId, checkpointId, request, idempotencyKey)
        ));
    }

}
