package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.entity.InventoryTransaction;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.repository.InventoryTransactionRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final InventoryTransactionRepository inventoryRepo; // 🚨 BƠM SỔ CÁI VÀO ĐÂY

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return repository.findAllByStockPriority(pageable).map(mapper::toDto);
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

        if (entity.getVariants() != null) {
            entity.getVariants().forEach(v -> v.setProduct(entity));
        }
        entity.setDelete(false);
        Product savedProduct = repository.save(entity);

        // 🚨 GHI SỔ CÁI: NHẬP KHO LẦN ĐẦU KHI TẠO SẢN PHẨM MỚI
        if (savedProduct.getVariants() != null) {
            for (ProductVariant v : savedProduct.getVariants()) {
                if (v.getQuantity() > 0) {
                    saveInventoryTransaction(v, v.getQuantity(), "INITIAL_STOCK");
                }
            }
        }

        return mapper.toDto(savedProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void updateProduct(Long id, ProductDTO dto) {
        Product existingProduct = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not found"));

        existingProduct.setProductName(dto.getProduct_name());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setTags(dto.getTags());
        existingProduct.setImageUrl(dto.getImage_url());

        if (dto.getVariants() != null) {
            List<Long> incomingVariantIds = dto.getVariants().stream()
                    .map(ProductVariantDTO::getId)
                    .filter(Objects::nonNull)
                    .toList();

            existingProduct.getVariants().removeIf(v -> v.getId() != null && !incomingVariantIds.contains(v.getId()));

            for (ProductVariantDTO vDto : dto.getVariants()) {
                if (vDto.getId() != null) {
                    ProductVariant existingVariant = existingProduct.getVariants().stream()
                            .filter(v -> vDto.getId().equals(v.getId()))
                            .findFirst()
                            .orElse(null);

                    if (existingVariant != null) {
                        // 🚨 KIỂM TRA BIẾN ĐỘNG KHO ĐỂ GHI SỔ
                        int oldQuantity = existingVariant.getQuantity();
                        int newQuantity = vDto.getQuantity();
                        int difference = newQuantity - oldQuantity;

                        existingVariant.setVariantName(vDto.getVariantName());
                        existingVariant.setPrice(vDto.getPrice());
                        existingVariant.setQuantity(newQuantity);
                        existingVariant.setAttributes(vDto.getAttributes());
                        existingVariant.setImageUrl(vDto.getImageUrl());

                        // Nếu Chủ shop điều chỉnh số lượng kho bằng tay -> Ghi sổ sao kê
                        if (difference != 0) {
                            saveInventoryTransaction(existingVariant, difference, "MANUAL_ADJUSTMENT");
                        }
                    }
                } else {
                    ProductVariant newVariant = new ProductVariant();
                    newVariant.setVariantName(vDto.getVariantName());
                    newVariant.setPrice(vDto.getPrice());
                    newVariant.setQuantity(vDto.getQuantity());
                    newVariant.setAttributes(vDto.getAttributes());
                    newVariant.setImageUrl(vDto.getImageUrl());
                    newVariant.setProduct(existingProduct);

                    existingProduct.getVariants().add(newVariant);

                    // 🚨 GHI SỔ CÁI KHI THÊM BIẾN THỂ MỚI
                    if (newVariant.getQuantity() > 0) {
                        saveInventoryTransaction(newVariant, newVariant.getQuantity(), "INITIAL_STOCK");
                    }
                }
            }
        } else {
            existingProduct.getVariants().clear();
        }

        repository.save(existingProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(Long id) {
        Product pro =  repository.findById(id).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"Không có sản phẩm"));
        pro.setDelete(true);
        repository.save(pro);
    }

    // ==========================================
    // HÀM TIỆN ÍCH GHI SỔ SAO KÊ KHO TỰ ĐỘNG
    // ==========================================
    private void saveInventoryTransaction(ProductVariant variant, int changeAmount, String type) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setProductVariant(variant);
        tx.setQuantityChange(changeAmount);
        tx.setRemainingStock(variant.getQuantity());
        tx.setTransactionType(type);
        tx.setCreatedAt(LocalDateTime.now());
        // Có thể lấy User từ SecurityContext để lưu vết ai là người sửa (tùy chọn)
        inventoryRepo.save(tx);
    }
}