package com.example.workflow.service;

import com.example.workflow.dto.BestSellerProductDTO;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.dto.StockImportRequest;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
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
    private final InventoryTransactionService inventoryTransactionService;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return repository.findAllByStockPriority(normalizePageable(pageable)).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "bestSellingProducts", key = "#period + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<BestSellerProductDTO> getBestSellingProducts(String period, Pageable pageable) {
        BestSellerRange range = resolveBestSellerRange(period);
        return inventoryRepo.findBestSellingProducts(range.fromTime(), range.toTime(), normalizePageable(pageable));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        return mapper.toDto(getActiveProduct(id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true)
    })
    public ProductDTO createProduct(ProductDTO dto, String userId) {
        User actor = getActor(userId);
        Product entity = mapper.toEntity(dto);
        prepareProductForCreate(entity);
        Product savedProduct = repository.saveAndFlush(entity);
        recordInitialStock(savedProduct.getVariants(), actor);

        return mapper.toDto(savedProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true),
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public void updateProduct(Long id, ProductDTO dto, String userId) {
        User actor = getActor(userId);
        Product existingProduct = getActiveProduct(id);
        List<InventoryLogEntry> inventoryLogs = new ArrayList<>();

        applyProductBasicInfo(existingProduct, dto);

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
                            .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VARIANT_NOT_IN_PRODUCT, variantDto.getId()));

                    int oldQuantity = existingVariant.getQuantity();
                    int newQuantity = variantDto.getQuantity();
                    int difference = newQuantity - oldQuantity;

                    applyVariantInfo(existingVariant, variantDto, existingProduct);

                    if (difference != 0) {
                        inventoryLogs.add(new InventoryLogEntry(existingVariant, difference, "MANUAL_ADJUSTMENT"));
                    }
                } else {
                    ProductVariant newVariant = createVariant(existingProduct, variantDto);
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
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public void updateProductBasicInfo(Long id, ProductDTO dto) {
        Product product = getActiveProduct(id);

        applyProductBasicInfo(product, dto);

        repository.save(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true),
            @CacheEvict(value = "product", key = "#productId")
    })
    public ProductDTO addVariant(Long productId, ProductVariantDTO dto, String userId) {
        User actor = getActor(userId);
        Product product = getActiveProduct(productId);

        ProductVariant variant = createVariant(product, dto);
        ProductVariant savedVariant = variantRepository.saveAndFlush(variant);
        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        product.getVariants().add(savedVariant);

        recordInitialStock(List.of(savedVariant), actor);

        return mapper.toDto(repository.saveAndFlush(product));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true),
            @CacheEvict(value = "product", allEntries = true)
    })
    public ProductVariantDTO importStock(Long variantId, StockImportRequest request, String userId) {
        User actor = getActor(userId);
        ProductVariant variant = getActiveVariant(variantId);

        variant.setQuantity(variant.getQuantity() + request.getQuantity());
        ProductVariant savedVariant = variantRepository.saveAndFlush(variant);
        saveInventoryTransaction(savedVariant, request.getQuantity(), "RESTOCK", actor);

        return mapper.variantToDto(savedVariant);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true),
            @CacheEvict(value = "product", key = "#id")
    })
    public void deleteProduct(Long id, String userId) {
        getActor(userId);
        Product product = getActiveProduct(id);
        product.setDelete(true);
        if (product.getVariants() != null) {
            product.getVariants().forEach(variant -> variant.setDelete(true));
        }
        repository.save(product);
    }

    private void applyProductBasicInfo(Product product, ProductDTO dto) {
        product.setProductName(dto.getProduct_name());
        product.setPrice(dto.getPrice());
        product.setTags(dto.getTags());
        product.setImageUrl(dto.getImage_url());
    }

    private void prepareProductForCreate(Product product) {
        product.setDelete(false);
        if (product.getVariants() == null) {
            return;
        }
        product.getVariants().forEach(variant -> applyVariantOwnership(product, variant));
    }

    private void applyVariantOwnership(Product product, ProductVariant variant) {
        variant.setProduct(product);
        variant.setDelete(false);
    }

    private void recordInitialStock(List<ProductVariant> variants, User actor) {
        if (variants == null) {
            return;
        }
        for (ProductVariant variant : variants) {
            if (variant.getQuantity() > 0) {
                saveInventoryTransaction(variant, variant.getQuantity(), "INITIAL_STOCK", actor);
            }
        }
    }

    private ProductVariant createVariant(Product product, ProductVariantDTO dto) {
        ProductVariant variant = new ProductVariant();
        applyVariantInfo(variant, dto, product);
        return variant;
    }

    private void applyVariantInfo(ProductVariant variant, ProductVariantDTO dto, Product product) {
        variant.setProduct(product);
        variant.setVariantName(dto.getVariantName());
        variant.setPrice(dto.getPrice());
        variant.setQuantity(dto.getQuantity());
        variant.setAttributes(dto.getAttributes());
        variant.setImageUrl(dto.getImageUrl());
        variant.setDelete(false);
    }

    private Product getActiveProduct(Long productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private ProductVariant getActiveVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND));
        if (variant.isDelete() || variant.getProduct() == null || variant.getProduct().isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND);
        }
        return variant;
    }

    private void saveInventoryTransaction(ProductVariant variant, int changeAmount, String type, User actor) {
        if (variant.getId() == null) {
            if (variant.getProduct() == null || variant.getProduct().getId() == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_VARIANT_MUST_BE_SAVED);
            }
            variant = variantRepository.saveAndFlush(variant);
        }

        inventoryTransactionService.record(null, variant, actor, changeAmount, type);
    }

    private User getActor(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND_WITH_ID, userId));
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
            default -> throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.INVALID_BEST_SELLER_PERIOD);
        };
    }

    private record BestSellerRange(LocalDateTime fromTime, LocalDateTime toTime) {
    }

    private record InventoryLogEntry(ProductVariant variant, int changeAmount, String type) {
    }
}
