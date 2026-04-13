package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return repository.findAllByStockPriority(pageable)
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not found"));
        return mapper.toDto(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO createProduct(ProductDTO dto) {
        Product entity = mapper.toEntity(dto);

        // Thiết lập mối quan hệ 2 chiều cho các biến thể khi tạo mới
        if (entity.getVariants() != null) {
            entity.getVariants().forEach(v -> v.setProduct(entity));
        }
        entity.setDelete(false);
        Product savedProduct = repository.save(entity);
        return mapper.toDto(savedProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void updateProduct(Long id, ProductDTO dto) {
        // 1. Tìm sản phẩm cũ
        Product existingProduct = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not found"));

        // 2. Cập nhật thông tin gốc (Cha)
        existingProduct.setProductName(dto.getProduct_name());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setTags(dto.getTags());
        existingProduct.setImageUrl(dto.getImage_url());

        // 3. Xử lý danh sách Biến thể (Con)
        if (dto.getVariants() != null) {
            List<Long> incomingVariantIds = dto.getVariants().stream()
                    .map(ProductVariantDTO::getId)
                    .filter(Objects::nonNull)
                    .toList();

            existingProduct.getVariants().removeIf(v -> v.getId() != null && !incomingVariantIds.contains(v.getId()));

            // B. DUYỆT ĐỂ THÊM/SỬA
            for (ProductVariantDTO vDto : dto.getVariants()) {
                if (vDto.getId() != null) {
                    // TRƯỜNG HỢP CẬP NHẬT BIẾN THỂ CŨ
                    ProductVariant existingVariant = existingProduct.getVariants().stream()
                            .filter(v -> vDto.getId().equals(v.getId()))
                            .findFirst()
                            .orElse(null);

                    if (existingVariant != null) {
                        existingVariant.setVariantName(vDto.getVariantName());
                        existingVariant.setPrice(vDto.getPrice());
                        existingVariant.setQuantity(vDto.getQuantity());
                        // ĐÃ FIX LỖI GÕ NHẦM Ở ĐÂY:
                        existingVariant.setAttributes(vDto.getAttributes());
                        existingVariant.setImageUrl(vDto.getImageUrl());
                    }
                } else {
                    // TRƯỜNG HỢP THÊM BIẾN THỂ MỚI
                    ProductVariant newVariant = new ProductVariant();
                    newVariant.setVariantName(vDto.getVariantName());
                    newVariant.setPrice(vDto.getPrice());
                    newVariant.setQuantity(vDto.getQuantity());
                    newVariant.setAttributes(vDto.getAttributes());
                    newVariant.setImageUrl(vDto.getImageUrl());
                    newVariant.setProduct(existingProduct);

                    existingProduct.getVariants().add(newVariant);
                }
            }
        } else {
            existingProduct.getVariants().clear();
        }

        // 4. Lưu toàn bộ xuống Database
        repository.save(existingProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(Long id) {
       Product pro =  repository.findById(id).orElseThrow(()
               -> new AppException(HttpStatus.NOT_FOUND,"Không có sản phẩm"));
       pro.setDelete(true);
       repository.save(pro);
    }
}