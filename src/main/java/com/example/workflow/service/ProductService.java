package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.entity.InventoryTransaction;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.InventoryTransactionRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final InventoryTransactionRepository inventoryRepo;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return repository.findAllByStockPriority(normalizePageable(pageable)).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "bestSellingProducts", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getBestSellingProducts(Pageable pageable) {
        return repository.findBestSellingProducts(OrderStatus.DELIVERED, normalizePageable(pageable)).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not found"));
        return mapper.toDto(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true)
    })
    public ProductDTO createProduct(ProductDTO dto, Long userId) {
        User actor = getActor(userId);
        Product entity = mapper.toEntity(dto);

        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> variant.setProduct(entity));
        }
        entity.setDelete(false);
        Product savedProduct = repository.save(entity);

        if (savedProduct.getVariants() != null) {
            for (ProductVariant variant : savedProduct.getVariants()) {
                if (variant.getQuantity() > 0) {
                    saveInventoryTransaction(variant, variant.getQuantity(), "INITIAL_STOCK", actor);
                }
            }
        }

        return mapper.toDto(savedProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void updateProduct(Long id, ProductDTO dto, Long userId) {
        User actor = getActor(userId);
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

            existingProduct.getVariants().removeIf(variant ->
                    variant.getId() != null && !incomingVariantIds.contains(variant.getId())
            );

            for (ProductVariantDTO variantDto : dto.getVariants()) {
                if (variantDto.getId() != null) {
                    ProductVariant existingVariant = existingProduct.getVariants().stream()
                            .filter(variant -> variantDto.getId().equals(variant.getId()))
                            .findFirst()
                            .orElse(null);

                    if (existingVariant != null) {
                        int oldQuantity = existingVariant.getQuantity();
                        int newQuantity = variantDto.getQuantity();
                        int difference = newQuantity - oldQuantity;

                        existingVariant.setVariantName(variantDto.getVariantName());
                        existingVariant.setPrice(variantDto.getPrice());
                        existingVariant.setQuantity(newQuantity);
                        existingVariant.setAttributes(variantDto.getAttributes());
                        existingVariant.setImageUrl(variantDto.getImageUrl());

                        if (difference != 0) {
                            saveInventoryTransaction(existingVariant, difference, "MANUAL_ADJUSTMENT", actor);
                        }
                    }
                } else {
                    ProductVariant newVariant = new ProductVariant();
                    newVariant.setVariantName(variantDto.getVariantName());
                    newVariant.setPrice(variantDto.getPrice());
                    newVariant.setQuantity(variantDto.getQuantity());
                    newVariant.setAttributes(variantDto.getAttributes());
                    newVariant.setImageUrl(variantDto.getImageUrl());
                    newVariant.setProduct(existingProduct);

                    existingProduct.getVariants().add(newVariant);

                    if (newVariant.getQuantity() > 0) {
                        saveInventoryTransaction(newVariant, newVariant.getQuantity(), "INITIAL_STOCK", actor);
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
            @CacheEvict(value = "bestSellingProducts", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(Long id, Long userId) {
        getActor(userId);
        Product product = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khong co san pham"));
        product.setDelete(true);
        repository.save(product);
    }

    private void saveInventoryTransaction(ProductVariant variant, int changeAmount, String type, User actor) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setProductVariant(variant);
        tx.setUser(actor);
        tx.setQuantityChange(changeAmount);
        tx.setRemainingStock(variant.getQuantity());
        tx.setTransactionType(type);
        tx.setCreatedAt(LocalDateTime.now());
        inventoryRepo.save(tx);
    }

    private User getActor(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
    }

    private Pageable normalizePageable(Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 20 : pageable.getPageSize();
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }
}
