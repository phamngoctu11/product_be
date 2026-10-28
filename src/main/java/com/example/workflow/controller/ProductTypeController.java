package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ProductTypeDTO;
import com.example.workflow.dto.SaveProductTypeRequest;
import com.example.workflow.service.ProductTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-types")
@RequiredArgsConstructor
@Validated
public class ProductTypeController {
    private final ProductTypeService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductTypeDTO>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success(service.listActive()));
    }

    @GetMapping("/management")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductTypeDTO>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(service.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductTypeDTO>> get(@Positive @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getActive(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductTypeDTO>> create(@Valid @RequestBody SaveProductTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductTypeDTO>> update(
            @Positive @PathVariable Long id,
            @Valid @RequestBody SaveProductTypeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductTypeDTO>> deactivate(@Positive @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.deactivate(id)));
    }
}
