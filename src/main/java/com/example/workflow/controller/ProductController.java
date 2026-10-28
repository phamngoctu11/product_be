package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.BestSellerProductDTO;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.dto.ProductSearchSuggestionsDTO;
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

import java.math.BigDecimal;
import java.util.List;

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
            @Valid @RequestBody ProductDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.createProduct(dto)));
    }

    // API GET cho phép tất cả mọi người (kể cả khách chưa đăng nhập) xem
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "productTypeIds", required = false) List<Long> productTypeIds,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getAllProducts(keyword, minPrice, maxPrice, pageable, productTypeIds)));
    }

    @GetMapping("/search/suggestions")
    public ResponseEntity<ApiResponse<ProductSearchSuggestionsDTO>> getSearchSuggestions(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "3") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getSearchSuggestions(keyword, limit)));
    }

    @GetMapping("/best-selling")
    public ResponseEntity<ApiResponse<Page<BestSellerProductDTO>>> getBestSellingProducts(
            @RequestParam(defaultValue = "day") String period,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getBestSellingProducts(period, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@Positive(message = "Mã sản phẩm phải là số dương.") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getProductById(id)));
    }

    // 🚨 BẢO MẬT: Chỉ MANAGER hoặc ADMIN mới được sửa sản phẩm
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @Positive(message = "Mã sản phẩm phải là số dương.") @PathVariable Long id,
            @Valid @RequestBody ProductDTO dto
    ) {
        service.updateProduct(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công."));
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> addVariant(
            @Positive(message = "Mã sản phẩm phải là số dương.") @PathVariable Long id,
            @Valid @RequestBody ProductVariantDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success("Tạo biến thể sản phẩm thành công.", service.addVariant(id, dto)));
    }

    // Only MANAGER or ADMIN can delete products.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Positive(message = "Mã sản phẩm phải là số dương.") @PathVariable Long id
    ) {
        service.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công."));
    }
}
