package com.example.workflow.service;

import com.example.workflow.dto.BestSellerProductDTO;
import com.example.workflow.dto.ProductDTO;
import com.example.workflow.dto.ProductVariantDTO;
import com.example.workflow.dto.ProductSearchSuggestionsDTO;
import com.example.workflow.dto.ProductTypeSearchSuggestionDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.ProductType;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderItemRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.ProductTypeRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.math.BigDecimal;
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
    private final OrderItemRepository orderItemRepository;
    private final CurrentUserService currentUserService;
    private final ProductVariantRepository variantRepository;
    private final ApplicationCacheService applicationCacheService;
    private final ProductTypeRepository productTypeRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "T(com.example.workflow.service.ProductService).productsCacheKey(#keyword, #minPrice, #maxPrice, #pageable)")
    public Page<ProductDTO> getAllProducts(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return repository.searchByAvailabilityPriority(
                normalizeSearchKeyword(keyword), normalizePrice(minPrice), normalizePrice(maxPrice),
                ProductAvailabilityStatus.ACCEPTING_ORDERS, PageableUtils.normalize(pageable, 20, 100)
        ).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "T(com.example.workflow.service.ProductService).productsCacheKey(#keyword, #minPrice, #maxPrice, #pageable, #productTypeIds)")
    public Page<ProductDTO> getAllProducts(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            List<Long> productTypeIds
    ) {
        List<Long> selectedTypeIds = productTypeIds == null ? List.of() : productTypeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (selectedTypeIds.isEmpty()) {
            return getAllProducts(keyword, minPrice, maxPrice, pageable);
        }

        return repository.searchByAvailabilityPriorityAndProductTypes(
                normalizeSearchKeyword(keyword), normalizePrice(minPrice), normalizePrice(maxPrice),
                ProductAvailabilityStatus.ACCEPTING_ORDERS, selectedTypeIds,
                PageableUtils.normalize(pageable, 20, 100)
        ).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProductSearchSuggestionsDTO getSearchSuggestions(String keyword, int requestedSize) {
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        if (normalizedKeyword == null || normalizedKeyword.length() < 2) {
            return new ProductSearchSuggestionsDTO(List.of(), List.of(), 0);
        }

        int productLimit = Math.min(Math.max(requestedSize, 1), 3);
        var productPage = repository.searchByAvailabilityPriority(
                normalizedKeyword, null, null, ProductAvailabilityStatus.ACCEPTING_ORDERS,
                PageRequest.of(0, productLimit)
        );
        var productTypes = productTypeRepository.findSearchSuggestions(
                        "%" + normalizedKeyword + "%", PageRequest.of(0, 8))
                .stream()
                .map(type -> new ProductTypeSearchSuggestionDTO(type.getId(), type.getCode(), type.getName()))
                .toList();

        return new ProductSearchSuggestionsDTO(
                productTypes,
                productPage.map(mapper::toDto).getContent(),
                productPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "bestSellingProducts", key = "#period + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<BestSellerProductDTO> getBestSellingProducts(String period, Pageable pageable) {
        BestSellerRange range = resolveBestSellerRange(period);
        return orderItemRepository.findBestSellingProducts(
                OrderStatus.DELIVERED,
                range.fromTime(),
                range.toTime(),
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductDTO getProductById(Long id) {
        return mapper.toDto(requireActiveProduct(id));
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        currentUserService.requireCurrentUserId();
        validateMadeDay(dto.getMadeDay());
        Product entity = mapper.toEntity(dto);
        applyCustomCatalogReferences(dto, entity, true);
        prepareProductForCreate(entity);
        Product savedProduct = repository.saveAndFlush(entity);

        applicationCacheService.evictProductCreated();
        return mapper.toDto(savedProduct);
    }

    @Transactional
    public void updateProduct(Long id, ProductDTO dto) {
        currentUserService.requireCurrentUserId();
        validateMadeDay(dto.getMadeDay());
        Product existingProduct = requireActiveProduct(id);
        mapper.updateCatalogInfo(dto, existingProduct);
        applyCustomCatalogReferences(dto, existingProduct, false);
        if (dto.getAvailabilityStatus() != null) {
            existingProduct.setAvailabilityStatus(dto.getAvailabilityStatus());
        }

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

                    mapper.updateVariant(variantDto, existingVariant);
                    applyVariantOwnership(existingProduct, existingVariant);
                } else {
                    ProductVariant newVariant = createVariant(existingProduct, variantDto);
                    ProductVariant savedVariant = variantRepository.saveAndFlush(newVariant);
                    existingProduct.getVariants().add(savedVariant);
                }
            }
        } else {
            existingProduct.getVariants().forEach(variant -> variant.setDelete(true));
        }

        repository.saveAndFlush(existingProduct);
        applicationCacheService.evictProductUpdated(id);
    }

    @Transactional
    public ProductDTO addVariant(Long productId, ProductVariantDTO dto) {
        currentUserService.requireCurrentUserId();
        Product product = requireActiveProduct(productId);

        ProductVariant variant = createVariant(product, dto);
        ProductVariant savedVariant = variantRepository.saveAndFlush(variant);
        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        product.getVariants().add(savedVariant);

        ProductDTO result = mapper.toDto(repository.saveAndFlush(product));
        applicationCacheService.evictProductVariantAdded(productId);
        return result;
    }

    @Transactional
    public void deleteProduct(Long id) {
        currentUserService.requireCurrentUserId();
        Product product = requireActiveProduct(id);
        product.setDelete(true);
        if (product.getVariants() != null) {
            product.getVariants().forEach(variant -> variant.setDelete(true));
        }
        repository.save(product);
        applicationCacheService.evictProductDeleted(id);
    }

    private void prepareProductForCreate(Product product) {
        product.setDelete(false);
        if (product.getAvailabilityStatus() == null) {
            product.setAvailabilityStatus(ProductAvailabilityStatus.ACCEPTING_ORDERS);
        }
        if (product.getVariants() == null) {
            return;
        }
        product.getVariants().forEach(variant -> applyVariantOwnership(product, variant));
    }

    private void applyCustomCatalogReferences(ProductDTO dto, Product product, boolean creating) {
        if (dto.getProductTypeId() != null) {
            ProductType type = productTypeRepository.findById(dto.getProductTypeId())
                    .filter(ProductType::isActive)
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_TYPE_NOT_FOUND));
            product.setProductType(type);
        } else if (creating) {
            product.setProductType(null);
        }
    }

    private void validateMadeDay(Double madeDay) {
        if (madeDay == null || !Double.isFinite(madeDay) || madeDay < 2) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Thời gian sản xuất phải là số hợp lệ và không nhỏ hơn 2 ngày.");
        }
    }

    private void applyVariantOwnership(Product product, ProductVariant variant) {
        variant.setProduct(product);
        variant.setDelete(false);
    }

    private ProductVariant createVariant(Product product, ProductVariantDTO dto) {
        ProductVariant variant = mapper.variantToEntity(dto);
        applyVariantOwnership(product, variant);
        return variant;
    }

    @Transactional(readOnly = true)
    public Product requireActiveProduct(Long productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    @Transactional(readOnly = true)
    public ProductVariant requireActiveVariant(Long variantId) {
        return variantRepository.findActiveById(variantId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND));
    }

    public static String productsCacheKey(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productsCacheKey(keyword, minPrice, maxPrice, pageable, List.of());
    }

    public static String productsCacheKey(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            List<Long> productTypeIds
    ) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int size = pageable == null ? 20 : Math.min(Math.max(pageable.getPageSize(), 1), 100);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedMinPrice = minPrice == null || minPrice.signum() < 0 ? "" : minPrice.stripTrailingZeros().toPlainString();
        String normalizedMaxPrice = maxPrice == null || maxPrice.signum() < 0 ? "" : maxPrice.stripTrailingZeros().toPlainString();
        String normalizedProductTypeIds = productTypeIds == null ? "" : productTypeIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return normalizedKeyword + '-' + normalizedMinPrice + '-' + normalizedMaxPrice + '-'
                + page + '-' + size + "-types-" + normalizedProductTypeIds;
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price == null || price.signum() < 0 ? null : price;
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
}
