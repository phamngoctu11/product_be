package com.example.workflow.service;

import com.example.workflow.dto.ProductDTO;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.User;
import com.example.workflow.entity.WishlistItem;
import com.example.workflow.mapper.ProductMapper;
import com.example.workflow.repository.WishlistItemRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ProductService productService;
    private final ProductMapper productMapper;
    private final CurrentUserService currentUserService;
    private final ApplicationCacheService applicationCacheService;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "wishlistProducts",
            key = "@currentUserService.requireCurrentUserId() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize",
            unless = "#result == null"
    )
    public Page<ProductDTO> getMyWishlist(Pageable pageable) {
        String userId = currentUserService.requireCurrentUserId();
        return wishlistItemRepository.findActiveByUserId(userId, PageableUtils.normalize(pageable, 20, 100))
                .map(WishlistItem::getProduct)
                .map(productMapper::toDto);
    }

    @Transactional
    public ProductDTO addToWishlist(Long productId) {
        String userId = currentUserService.requireCurrentUserId();
        Product product = productService.requireActiveProduct(productId);

        ProductDTO response = wishlistItemRepository.findByUser_IdAndProduct_Id(userId, productId)
                .map(WishlistItem::getProduct)
                .map(productMapper::toDto)
                .orElseGet(() -> createWishlistItem(product));
        applicationCacheService.evictWishlistChanged(userId, productId);
        return response;
    }

    @Transactional
    public void removeFromWishlist(Long productId) {
        String userId = currentUserService.requireCurrentUserId();
        wishlistItemRepository.deleteByUser_IdAndProduct_Id(userId, productId);
        applicationCacheService.evictWishlistChanged(userId, productId);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "wishlistStatus",
            key = "@currentUserService.requireCurrentUserId() + '-' + #productId"
    )
    public boolean isInMyWishlist(Long productId) {
        String userId = currentUserService.requireCurrentUserId();
        return wishlistItemRepository.findExistingProductIdsInWishlist(userId, List.of(productId)).contains(productId);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "wishlistStatusBatch",
            key = "@currentUserService.requireCurrentUserId() + '-' + T(com.example.workflow.service.WishlistService).cacheKeyForProductIds(#productIds)",
            unless = "#result == null || #result.isEmpty()"
    )
    public Map<Long, Boolean> getMyWishlistStatus(Collection<Long> productIds) {
        List<Long> normalizedProductIds = normalizeProductIds(productIds);
        if (normalizedProductIds.isEmpty()) {
            return Map.of();
        }

        String userId = currentUserService.requireCurrentUserId();
        Set<Long> favoriteProductIds = wishlistItemRepository.findExistingProductIdsInWishlist(userId, normalizedProductIds);

        Map<Long, Boolean> statusByProductId = new LinkedHashMap<>();
        for (Long productId : normalizedProductIds) {
            statusByProductId.put(productId, favoriteProductIds.contains(productId));
        }
        return statusByProductId;
    }

    private ProductDTO createWishlistItem(Product product) {
        User user = currentUserService.requireCurrentUser();
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        wishlistItemRepository.save(item);

        return productMapper.toDto(product);
    }
@Transactional(readOnly = true)
protected List<Long> normalizeProductIds(Collection<Long> productIds) {
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
