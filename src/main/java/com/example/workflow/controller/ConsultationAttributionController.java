package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ConsultationReviewDTO;
import com.example.workflow.dto.ConsultationReviewRequest;
import com.example.workflow.dto.ConsultationSaleAttributionDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.service.ConsultationAttributionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultation-attributions")
@RequiredArgsConstructor
@Validated
public class ConsultationAttributionController {
    private final ConsultationAttributionService attributionService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ConsultationSaleAttributionDTO>>> getMyAttributions(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(attributionService.getMyAttributions(pageable)));
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ConsultationSaleAttributionDTO>>> getStaffAttributions(
            @PathVariable String staffId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(attributionService.getStaffAttributions(staffId, pageable)));
    }

    @PostMapping("/{attributionId}/review")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<ConsultationReviewDTO>> createReview(
            @Positive @PathVariable Long attributionId,
            @Valid @RequestBody ConsultationReviewRequest request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Danh gia tu van thanh cong.", attributionService.createReview(attributionId, request)));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, e.getMessage());
        }
    }

    @GetMapping("/reviews/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ConsultationReviewDTO>>> getProductReviews(
            @Positive @PathVariable Long productId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(attributionService.getProductReviews(productId, pageable)));
    }

    @GetMapping("/reviews/staff/{staffId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ConsultationReviewDTO>>> getStaffReviews(
            @PathVariable String staffId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(attributionService.getStaffReviews(staffId, pageable)));
    }
}
