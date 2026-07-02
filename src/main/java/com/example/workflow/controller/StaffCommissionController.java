package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.StaffCommissionDetailDTO;
import com.example.workflow.dto.StaffCommissionSummaryDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.CommissionPeriod;
import com.example.workflow.nume.ConsultationAttributionStatus;
import com.example.workflow.service.StaffCommissionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/consultation-commissions")
@RequiredArgsConstructor
@Validated
public class StaffCommissionController {
    private final StaffCommissionService staffCommissionService;

    @GetMapping("/me/summary")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<StaffCommissionSummaryDTO>> getMySummary(
            @RequestParam(defaultValue = "MONTH") String period,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                staffCommissionService.getMySummary(parsePeriod(period), from, to)
        ));
    }

    @GetMapping("/me/details")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseEntity<ApiResponse<Page<StaffCommissionDetailDTO>>> getMyDetails(
            @RequestParam(defaultValue = "MONTH") String period,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "CONFIRMED") String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                staffCommissionService.getMyDetails(parsePeriod(period), from, to, parseStatus(status), pageable)
        ));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<StaffCommissionSummaryDTO>>> getStaffSummaries(
            @RequestParam(defaultValue = "MONTH") String period,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                staffCommissionService.getStaffSummaries(parsePeriod(period), from, to, pageable)
        ));
    }

    @GetMapping("/staff/{staffId}/summary")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffCommissionSummaryDTO>> getStaffSummary(
            @PathVariable String staffId,
            @RequestParam(defaultValue = "MONTH") String period,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                staffCommissionService.getStaffSummary(staffId, parsePeriod(period), from, to)
        ));
    }

    @GetMapping("/staff/{staffId}/details")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<StaffCommissionDetailDTO>>> getStaffDetails(
            @PathVariable String staffId,
            @RequestParam(defaultValue = "MONTH") String period,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "CONFIRMED") String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                staffCommissionService.getStaffDetails(staffId, parsePeriod(period), from, to, parseStatus(status), pageable)
        ));
    }

    @PostMapping("/summaries/rebuild")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> rebuildSummaries(
            @RequestParam(defaultValue = "MONTH") String period,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate to
    ) {
        int refreshedDays = staffCommissionService.rebuildSummaries(parsePeriod(period), from, to);
        return ResponseEntity.ok(ApiResponse.success("Rebuilt staff commission summaries.", refreshedDays));
    }

    private CommissionPeriod parsePeriod(String period) {
        try {
            return CommissionPeriod.valueOf(period.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Period must be DAY, WEEK, or MONTH.");
        }
    }

    private ConsultationAttributionStatus parseStatus(String status) {
        try {
            return ConsultationAttributionStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Status must be PENDING, CONFIRMED, or CANCELLED.");
        }
    }
}
