package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.BestSellerProductDTO;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.dto.StockImportRequest;
import com.example.workflow.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final ProductService service;

    // 🚨 BẢO MẬT: Chỉ MANAGER (Chủ shop) hoặc ADMIN mới được tạo sản phẩm
    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestParam("userId") String userId,
            @Valid @RequestBody ProductDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.createProduct(dto, userId)));
    }

    // API GET cho phép tất cả mọi người (kể cả khách chưa đăng nhập) xem
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getAllProducts(pageable)));
    }

    @GetMapping("/best-selling")
    public ResponseEntity<ApiResponse<Page<BestSellerProductDTO>>> getBestSellingProducts(
            @RequestParam(defaultValue = "day") String period,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getBestSellingProducts(period, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@Positive(message = "Product id must be positive") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getProductById(id)));
    }

    // 🚨 BẢO MẬT: Chỉ MANAGER hoặc ADMIN mới được sửa sản phẩm
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @Positive(message = "Product id must be positive") @PathVariable Long id,
            @RequestParam("userId") String userId,
            @Valid @RequestBody ProductDTO dto
    ) {
        service.updateProduct(id, dto, userId);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully"));
    }

    // STAFF can update basic product info, add variants, and import stock; product create/delete stays manager/admin only.
    @PatchMapping("/{id}/staff-info")
    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProductBasicInfo(
            @Positive(message = "Product id must be positive") @PathVariable Long id,
            @Valid @RequestBody ProductDTO dto
    ) {
        service.updateProductBasicInfo(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Product information updated successfully"));
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> addVariant(
            @Positive(message = "Product id must be positive") @PathVariable Long id,
            @RequestParam("userId") String userId,
            @Valid @RequestBody ProductVariantDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success("Variant created successfully", service.addVariant(id, dto, userId)));
    }

    @PostMapping("/variants/{variantId}/restock")
    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> importStock(
            @Positive(message = "Variant id must be positive") @PathVariable Long variantId,
            @RequestParam("userId") String userId,
            @Valid @RequestBody StockImportRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Stock imported successfully", service.importStock(variantId, request, userId)));
    }

    // Only MANAGER or ADMIN can delete products.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Positive(message = "Product id must be positive") @PathVariable Long id,
            @RequestParam("userId") String userId
    ) {
        service.deleteProduct(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }
}
