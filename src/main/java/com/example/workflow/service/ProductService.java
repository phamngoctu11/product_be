package com.example.workflow.service;

import com.example.workflow.dto.BestSellerProductDTO;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.entity.InventoryTransaction;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.repository.InventoryTransactionRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.ProductVariantRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final InventoryTransactionRepository inventoryRepo;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return repository.findAllByStockPriority(normalizePageable(pageable)).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BestSellerProductDTO> getBestSellingProducts(String period, Pageable pageable) {
        BestSellerRange range = resolveBestSellerRange(period);
        return inventoryRepo.findBestSellingProducts(range.fromTime(), range.toTime(), normalizePageable(pageable));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not found"));
        if (product.isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Product not found");
        }
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
            entity.getVariants().forEach(variant -> {
                variant.setProduct(entity);
                variant.setDelete(false);
            });
        }
        entity.setDelete(false);
        Product savedProduct = repository.saveAndFlush(entity);

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
        if (existingProduct.isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Product not found");
        }
        List<InventoryLogEntry> inventoryLogs = new ArrayList<>();

        existingProduct.setProductName(dto.getProduct_name());
        existingProduct.setPrice(dto.getPrice());
        existingProduct.setTags(dto.getTags());
        existingProduct.setImageUrl(dto.getImage_url());

        if (dto.getVariants() != null) {
            List<Long> incomingVariantIds = dto.getVariants().stream()
                    .map(ProductVariantDTO::getId)
                    .filter(Objects::nonNull)
                    .toList();

            existingProduct.getVariants().stream()
                    .filter(variant -> variant.getId() != null && !incomingVariantIds.contains(variant.getId()))
                    .forEach(variant -> variant.setDelete(true));

            for (ProductVariantDTO variantDto : dto.getVariants()) {
                if (variantDto.getId() != null) {
                    ProductVariant existingVariant = existingProduct.getVariants().stream()
                            .filter(variant -> variantDto.getId().equals(variant.getId()))
                            .findFirst()
                            .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Variant does not belong to this product: " + variantDto.getId()));

                    int oldQuantity = existingVariant.getQuantity();
                    int newQuantity = variantDto.getQuantity();
                    int difference = newQuantity - oldQuantity;

                    existingVariant.setVariantName(variantDto.getVariantName());
                    existingVariant.setPrice(variantDto.getPrice());
                    existingVariant.setQuantity(newQuantity);
                    existingVariant.setAttributes(variantDto.getAttributes());
                    existingVariant.setImageUrl(variantDto.getImageUrl());
                    existingVariant.setDelete(false);

                    if (difference != 0) {
                        inventoryLogs.add(new InventoryLogEntry(existingVariant, difference, "MANUAL_ADJUSTMENT"));
                    }
                } else {
                    ProductVariant newVariant = new ProductVariant();
                    newVariant.setVariantName(variantDto.getVariantName());
                    newVariant.setPrice(variantDto.getPrice());
                    newVariant.setQuantity(variantDto.getQuantity());
                    newVariant.setAttributes(variantDto.getAttributes());
                    newVariant.setImageUrl(variantDto.getImageUrl());
                    newVariant.setProduct(existingProduct);
                    newVariant.setDelete(false);

                    ProductVariant savedVariant = variantRepository.saveAndFlush(newVariant);
                    existingProduct.getVariants().add(savedVariant);

                    if (savedVariant.getQuantity() > 0) {
                        inventoryLogs.add(new InventoryLogEntry(savedVariant, savedVariant.getQuantity(), "INITIAL_STOCK"));
                    }
                }
            }
        } else {
            existingProduct.getVariants().forEach(variant -> variant.setDelete(true));
        }

        repository.saveAndFlush(existingProduct);

        for (InventoryLogEntry log : inventoryLogs) {
            saveInventoryTransaction(log.variant(), log.changeAmount(), log.type(), actor);
        }
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
        if (product.getVariants() != null) {
            product.getVariants().forEach(variant -> variant.setDelete(true));
        }
        repository.save(product);
    }

    private void saveInventoryTransaction(ProductVariant variant, int changeAmount, String type, User actor) {
        if (variant.getId() == null) {
            if (variant.getProduct() == null || variant.getProduct().getId() == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Product variant must belong to a saved product before inventory transaction is logged");
            }
            variant = variantRepository.saveAndFlush(variant);
        }

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

    private BestSellerRange resolveBestSellerRange(String period) {
        String normalizedPeriod = period == null ? "day" : period.trim().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();

        return switch (normalizedPeriod) {
            case "day", "daily", "ngay" -> {
                LocalDate yesterday = today.minusDays(1);
                yield new BestSellerRange(yesterday.atStartOfDay(), today.atStartOfDay());
            }
            case "week", "weekly", "tuan" -> {
                LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate previousWeekMonday = currentWeekMonday.minusWeeks(1);
                yield new BestSellerRange(previousWeekMonday.atStartOfDay(), currentWeekMonday.atStartOfDay());
            }
            case "month", "monthly", "thang" -> {
                LocalDate currentMonthStart = today.withDayOfMonth(1);
                LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
                yield new BestSellerRange(previousMonthStart.atStartOfDay(), currentMonthStart.atStartOfDay());
            }
            default -> throw new AppException(HttpStatus.BAD_REQUEST, "Period must be one of: day, week, month");
        };
    }

    private record BestSellerRange(LocalDateTime fromTime, LocalDateTime toTime) {
    }

    private record InventoryLogEntry(ProductVariant variant, int changeAmount, String type) {
    }
}
