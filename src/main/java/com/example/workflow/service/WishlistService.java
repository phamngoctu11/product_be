package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.User;
import com.example.workflow.entity.WishlistItem;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.WishlistItemRepository;
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final AuthService authService;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "wishlistProducts",
            key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize",
            unless = "#result == null"
    )
    public Page<ProductDTO> getMyWishlist(Pageable pageable) {
        String userId = authService.getCurrentUserId();
        return wishlistItemRepository.findActiveByUserId(userId, normalizePageable(pageable))
                .map(WishlistItem::getProduct)
                .map(productMapper::toDto);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wishlistProducts", allEntries = true),
            @CacheEvict(value = "wishlistStatus", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #productId"),
            @CacheEvict(value = "wishlistStatusBatch", allEntries = true)
    })
    public ProductDTO addToWishlist(Long productId) {
        String userId = authService.getCurrentUserId();
        Product product = getActiveProduct(productId);

        return wishlistItemRepository.findByUser_IdAndProduct_Id(userId, productId)
                .map(WishlistItem::getProduct)
                .map(productMapper::toDto)
                .orElseGet(() -> createWishlistItem(product));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wishlistProducts", allEntries = true),
            @CacheEvict(value = "wishlistStatus", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #productId"),
            @CacheEvict(value = "wishlistStatusBatch", allEntries = true)
    })
    public void removeFromWishlist(Long productId) {
        String userId = authService.getCurrentUserId();
        wishlistItemRepository.deleteByUser_IdAndProduct_Id(userId, productId);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "wishlistStatus",
            key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #productId"
    )
    public boolean isInMyWishlist(Long productId) {
        String userId = authService.getCurrentUserId();
        return wishlistItemRepository.findExistingProductIdsInWishlist(userId, List.of(productId)).contains(productId);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "wishlistStatusBatch",
            key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + T(com.example.workflow.service.WishlistService).cacheKeyForProductIds(#productIds)",
            unless = "#result == null || #result.isEmpty()"
    )
    public Map<Long, Boolean> getMyWishlistStatus(Collection<Long> productIds) {
        List<Long> normalizedProductIds = normalizeProductIds(productIds);
        if (normalizedProductIds.isEmpty()) {
            return Map.of();
        }

        String userId = authService.getCurrentUserId();
        Set<Long> favoriteProductIds = wishlistItemRepository.findExistingProductIdsInWishlist(userId, normalizedProductIds);

        Map<Long, Boolean> statusByProductId = new LinkedHashMap<>();
        for (Long productId : normalizedProductIds) {
            statusByProductId.put(productId, favoriteProductIds.contains(productId));
        }
        return statusByProductId;
    }

    private ProductDTO createWishlistItem(Product product) {
        User user = authService.getCurrentUser();
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        wishlistItemRepository.save(item);

        return productMapper.toDto(product);
    }
@Transactional(readOnly = true)
protected Product getActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private Pageable normalizePageable(Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 20 : pageable.getPageSize();
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    private List<Long> normalizeProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        return productIds.stream()
                .filter(Objects::nonNull)
                .filter(productId -> productId > 0)
                .distinct()
                .limit(100)
                .toList();
    }

    public static String cacheKeyForProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return "";
        }

        return productIds.stream()
                .filter(Objects::nonNull)
                .filter(productId -> productId > 0)
                .distinct()
                .sorted()
                .limit(100)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }
}
