package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.WorkflowOperationsSummaryDTO;
import com.example.workflow.dto.WorkflowDlqIncidentDTO;
import com.example.workflow.dto.WorkflowOutboxIncidentDTO;
import com.example.workflow.service.consistency.OutboxRecoveryService;
import com.example.workflow.service.redis.RedisStreamOperationsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/workflow-operations")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
public class WorkflowOperationsController {
    private final OutboxRecoveryService recoveryService;
    private final RedisStreamOperationsService redisOperations;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<WorkflowOperationsSummaryDTO>> summary() {
        return ResponseEntity.ok(ApiResponse.success(recoveryService.summary()));
    }

    @GetMapping("/outbox/incidents")
    public ResponseEntity<ApiResponse<List<WorkflowOutboxIncidentDTO>>> incidents(
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(recoveryService.incidents(limit)));
    }

    @PostMapping("/outbox/{eventId}/requeue")
    public ResponseEntity<ApiResponse<Boolean>> requeue(
            @PathVariable @NotBlank @Size(max = 36) String eventId
    ) {
        boolean requeued = recoveryService.requeue(eventId);
        return ResponseEntity.ok(ApiResponse.success(
                requeued ? "Sự kiện đã được đưa vào hàng đợi để thử lại." : "Không tìm thấy sự kiện trong hàng đợi.",
                requeued
        ));
    }

    @GetMapping("/dlq/incidents")
    public ResponseEntity<ApiResponse<List<WorkflowDlqIncidentDTO>>> dlqIncidents(
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(redisOperations.incidents(limit)));
    }

    @GetMapping("/dlq/count")
    public ResponseEntity<ApiResponse<Long>> dlqCount() {
        return ResponseEntity.ok(ApiResponse.success(redisOperations.dlqSize()));
    }

    @PostMapping("/dlq/{recordId}/replay")
    public ResponseEntity<ApiResponse<Boolean>> replayDlq(
            @PathVariable @NotBlank @Size(max = 64) String recordId
    ) {
        boolean replayed = redisOperations.replay(recordId);
        return ResponseEntity.ok(ApiResponse.success(
                replayed ? "Đã phát lại sự kiện trong hàng đợi lỗi." : "Không tìm thấy sự kiện trong hàng đợi lỗi.", replayed
        ));
    }
}
