package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.CreateCustomRequest;
import com.example.workflow.dto.CustomRequestDTO;
import com.example.workflow.dto.SubmitCustomRequest;
import com.example.workflow.dto.UpdateCustomRequest;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.service.CustomRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/custom-requests")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('USER')")
public class CustomRequestController {
    private final CustomRequestService customRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomRequestDTO>> create(
            @Valid @RequestBody CreateCustomRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        CustomRequestDTO response = customRequestService.create(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, HttpStatus.CREATED.value(), "Custom draft created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomRequestDTO>>> list(
            @RequestParam(required = false) CustomRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(customRequestService.list(status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomRequestDTO>> get(@Positive @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customRequestService.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomRequestDTO>> update(
            @Positive @PathVariable Long id,
            @Valid @RequestBody UpdateCustomRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Custom draft updated", customRequestService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Positive @PathVariable Long id,
            @PositiveOrZero @RequestParam Long expectedVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        customRequestService.delete(id, expectedVersion, idempotencyKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<CheckoutResponseDTO>> submit(
            @Positive @PathVariable Long id,
            @Valid @RequestBody SubmitCustomRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        CheckoutResponseDTO response = customRequestService.submit(id, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, HttpStatus.CREATED.value(), response.getMessage(), response));
    }
}
