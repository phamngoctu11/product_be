package com.example.workflow.controller;

import com.example.workflow.dto.ApiResponse;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(service.createProduct(dto)));
    }

    // API GET cho phép tất cả mọi người (kể cả khách chưa đăng nhập) xem
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getAllProducts(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@Positive(message = "Product id must be positive") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getProductById(id)));
    }

    // 🚨 BẢO MẬT: Chỉ MANAGER hoặc ADMIN mới được sửa sản phẩm
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProduct(@Positive(message = "Product id must be positive") @PathVariable Long id,
                                              @Valid @RequestBody ProductDTO dto) {
        service.updateProduct(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully"));
    }

    // 🚨 BẢO MẬT: Chỉ MANAGER hoặc ADMIN mới được xóa
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@Positive(message = "Product id must be positive") @PathVariable Long id) {
        service.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }
}
