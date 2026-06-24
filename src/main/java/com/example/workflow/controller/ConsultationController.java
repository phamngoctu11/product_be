package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ConsultationCreateRequest;
import com.example.workflow.dto.ConsultationRequestDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.ConsultationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Validated
public class ConsultationController {
    private final ConsultationService consultationService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ConsultationRequestDTO>> createRequest(
            @Valid @RequestBody ConsultationCreateRequest request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Tao yeu cau tu van thanh cong.", consultationService.createRequest(request)));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }

    @GetMapping("/waiting")
    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ConsultationRequestDTO>>> getWaitingRequests(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(consultationService.getWaitingRequests(pageable)));
    }

    @GetMapping("/staff/me")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Page<ConsultationRequestDTO>>> getMyAssignedRequests(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(consultationService.getMyAssignedRequests(pageable)));
    }

    @PostMapping("/{requestId}/claim")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<ConsultationRequestDTO>> claimRequest(
            @Positive @PathVariable Long requestId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Nhan yeu cau tu van thanh cong.", consultationService.claimRequest(requestId)));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }

    @PostMapping("/{requestId}/assign")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ConsultationRequestDTO>> assignRequest(
            @Positive @PathVariable Long requestId,
            @Positive @RequestParam Long staffId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Gan yeu cau tu van cho staff thanh cong.", consultationService.assignRequest(requestId, staffId)));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }

    @PostMapping("/{requestId}/close")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ConsultationRequestDTO>> closeMyRequest(
            @Positive @PathVariable Long requestId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Dong doan chat tu van thanh cong.", consultationService.closeMyRequest(requestId)));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }
}
