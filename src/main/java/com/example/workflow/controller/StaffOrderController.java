package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ClaimOrderRequest;
import com.example.workflow.dto.OrderAssignmentResultDTO;
import com.example.workflow.dto.OrderAssignmentQueueDTO;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.dto.StartProductionRequest;
import com.example.workflow.dto.StartProductionResultDTO;
import com.example.workflow.dto.SubmitProductionCheckpointRequest;
import com.example.workflow.dto.ProductionCheckpointDTO;
import com.example.workflow.dto.ChangeRequestDTO;
import com.example.workflow.dto.ChangeRequestDecisionRequest;
import com.example.workflow.service.OrderService;
import com.example.workflow.service.OrderAssignmentService;
import com.example.workflow.service.ProductionStartService;
import com.example.workflow.service.ProductionCheckpointService;
import com.example.workflow.service.ChangeRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/api/orders/staff")
@RequiredArgsConstructor
@Validated
public class StaffOrderController {
    private final OrderService orderService;
    private final OrderAssignmentService assignmentService;
    private final ProductionStartService productionStartService;
    private final ProductionCheckpointService productionCheckpointService;
    private final ChangeRequestService changeRequestService;

    @GetMapping("/claimable")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Page<OrderAssignmentQueueDTO>>> getClaimableOrders(
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

    @PostMapping("/{orderId}/start-production")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<StartProductionResultDTO>> startProduction(
            @Positive @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody StartProductionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Production started.",
                productionStartService.start(orderId, request, idempotencyKey)
        ));
    }

    @PostMapping("/{orderId}/items/{orderItemId}/checkpoints")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<ProductionCheckpointDTO>> submitProductionCheckpoint(
            @Positive @PathVariable Long orderId,
            @Positive @PathVariable Long orderItemId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody SubmitProductionCheckpointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Production checkpoint submitted.",
                productionCheckpointService.submit(orderId, orderItemId, request, idempotencyKey)
        ));
    }

    @PostMapping("/{orderId}/change-requests/{changeRequestId}/decision")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<ChangeRequestDTO>> decideChangeRequest(
            @Positive @PathVariable Long orderId,
            @Positive @PathVariable Long changeRequestId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
            @Valid @RequestBody ChangeRequestDecisionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Yêu cầu chỉnh sửa đã được xử lý.",
                changeRequestService.decide(orderId, changeRequestId, request, idempotencyKey)
        ));
    }

}
