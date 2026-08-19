package com.example.workflow.service;

import com.example.workflow.dto.ProductReviewDTO;
import com.example.workflow.dto.ProductReviewRequest;
import com.example.workflow.dto.ProductReviewSummaryDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductReview;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.ProductReviewStatus;
import com.example.workflow.repository.OrderItemRepository;
import com.example.workflow.repository.ProductReviewRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewService {
    private static final int MAX_IMAGE_COUNT = 5;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ProductReviewRepository productReviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final ApplicationCacheService applicationCacheService;

    @Transactional
    public ProductReviewDTO createForOrderItem(Long orderItemId, ProductReviewRequest request) {
        User currentUser = authService.getCurrentUser();
        OrderItem orderItem = orderItemRepository.findReviewTargetById(orderItemId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_ITEM_NOT_FOUND));
        Order order = orderItem.getOrder();
        ProductVariant variant = orderItem.getProductVariant();
        Product product = variant.getProduct();

        validateReviewTarget(order, orderItem, currentUser);
        validateReviewContent(request);
        if (productReviewRepository.existsByOrderItem_Id(orderItemId)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCT_REVIEW_ALREADY_EXISTS);
        }

        ProductReview review = new ProductReview();
        review.setOrder(order);
        review.setOrderItem(orderItem);
        review.setUser(currentUser);
        review.setProduct(product);
        review.setProductVariant(variant);
        applyRequest(review, request);

        ProductReviewDTO dto = toDto(productReviewRepository.save(review));
        applicationCacheService.evictProductReviewChangedByUser(currentUser.getId());
        return dto;
    }

    @Transactional
    public ProductReviewDTO updateMyReview(Long reviewId, ProductReviewRequest request) {
        String currentUserId = authService.getCurrentUserId();
        validateReviewContent(request);
        ProductReview review = productReviewRepository.findByIdAndUser_Id(reviewId, currentUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_REVIEW_NOT_FOUND));
        applyRequest(review, request);
        ProductReviewDTO dto = toDto(productReviewRepository.save(review));
        applicationCacheService.evictProductReviewChangedByUser(currentUserId);
        return dto;
    }

    @Transactional
    public ProductReviewDTO hideReview(Long reviewId, String reason) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_REVIEW_NOT_FOUND));
        review.setStatus(ProductReviewStatus.HIDDEN);
        review.setHiddenReason(StringUtils.hasText(reason) ? reason.trim() : null);
        ProductReviewDTO dto = toDto(productReviewRepository.save(review));
        applicationCacheService.evictProductReviewChanged();
        return dto;
    }

    @Transactional
    public ProductReviewDTO restoreReview(Long reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_REVIEW_NOT_FOUND));
        review.setStatus(ProductReviewStatus.VISIBLE);
        review.setHiddenReason(null);
        ProductReviewDTO dto = toDto(productReviewRepository.save(review));
        applicationCacheService.evictProductReviewChanged();
        return dto;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productReviews", key = "'product-' + #productId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductReviewDTO> getProductReviews(Long productId, Pageable pageable) {
        return productReviewRepository
                .findByProduct_IdAndStatusOrderByCreatedAtDesc(productId, ProductReviewStatus.VISIBLE, normalizePageable(pageable))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productReviews", key = "'variant-' + #variantId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductReviewDTO> getVisibleVariantReviews(Long variantId, Pageable pageable) {
        return productReviewRepository
                .findByProductVariant_IdAndStatusOrderByCreatedAtDesc(variantId, ProductReviewStatus.VISIBLE, normalizePageable(pageable))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewDTO> getManageableVariantReviews(Long variantId, Pageable pageable) {
        return productReviewRepository
                .findByProductVariant_IdOrderByCreatedAtDesc(variantId, normalizePageable(pageable))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productReviewSummaries", key = "'product-' + #productId")
    public ProductReviewSummaryDTO getProductSummary(Long productId) {
        return buildSummary(productId, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productReviewSummaries", key = "'variant-' + #variantId")
    public ProductReviewSummaryDTO getVariantSummary(Long variantId) {
        return buildSummary(null, variantId);
    }

    private void validateReviewTarget(Order order, OrderItem orderItem, User currentUser) {
        if (order == null || order.getUser() == null || !order.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_REVIEW_NOT_ALLOWED);
        }
        Integer receivedQuantity = orderItem.getReceivedQuantity();
        if (receivedQuantity != null && receivedQuantity <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_REVIEW_NOT_ALLOWED);
        }
    }

    private void validateReviewContent(ProductReviewRequest request) {
        List<String> images = normalizeImageUrls(request.imageUrls());
        if (request.rating() == null && !StringUtils.hasText(request.comment()) && images.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_REVIEW_CONTENT_REQUIRED);
        }
    }

    private void applyRequest(ProductReview review, ProductReviewRequest request) {
        review.setRating(request.rating());
        review.setComment(StringUtils.hasText(request.comment()) ? request.comment().trim() : null);
        review.setImageUrls(serializeImageUrls(normalizeImageUrls(request.imageUrls())));
    }

    private ProductReviewSummaryDTO buildSummary(Long productId, Long variantId) {
        long reviewCount;
        long ratingCount;
        double averageRating;
        long fiveStarCount;
        long fourStarCount;
        long threeStarCount;
        long twoStarCount;
        long oneStarCount;

        if (variantId == null) {
            reviewCount = productReviewRepository.countByProduct_IdAndStatus(productId, ProductReviewStatus.VISIBLE);
            ratingCount = productReviewRepository.countByProduct_IdAndStatusAndRatingIsNotNull(productId, ProductReviewStatus.VISIBLE);
            averageRating = productReviewRepository.averageRatingByProduct(productId, ProductReviewStatus.VISIBLE);
            fiveStarCount = productReviewRepository.countByProduct_IdAndStatusAndRating(productId, ProductReviewStatus.VISIBLE, 5);
            fourStarCount = productReviewRepository.countByProduct_IdAndStatusAndRating(productId, ProductReviewStatus.VISIBLE, 4);
            threeStarCount = productReviewRepository.countByProduct_IdAndStatusAndRating(productId, ProductReviewStatus.VISIBLE, 3);
            twoStarCount = productReviewRepository.countByProduct_IdAndStatusAndRating(productId, ProductReviewStatus.VISIBLE, 2);
            oneStarCount = productReviewRepository.countByProduct_IdAndStatusAndRating(productId, ProductReviewStatus.VISIBLE, 1);
        } else {
            reviewCount = productReviewRepository.countByProductVariant_IdAndStatus(variantId, ProductReviewStatus.VISIBLE);
            ratingCount = productReviewRepository.countByProductVariant_IdAndStatusAndRatingIsNotNull(variantId, ProductReviewStatus.VISIBLE);
            averageRating = productReviewRepository.averageRatingByVariant(variantId, ProductReviewStatus.VISIBLE);
            fiveStarCount = productReviewRepository.countByProductVariant_IdAndStatusAndRating(variantId, ProductReviewStatus.VISIBLE, 5);
            fourStarCount = productReviewRepository.countByProductVariant_IdAndStatusAndRating(variantId, ProductReviewStatus.VISIBLE, 4);
            threeStarCount = productReviewRepository.countByProductVariant_IdAndStatusAndRating(variantId, ProductReviewStatus.VISIBLE, 3);
            twoStarCount = productReviewRepository.countByProductVariant_IdAndStatusAndRating(variantId, ProductReviewStatus.VISIBLE, 2);
            oneStarCount = productReviewRepository.countByProductVariant_IdAndStatusAndRating(variantId, ProductReviewStatus.VISIBLE, 1);
        }

        return new ProductReviewSummaryDTO(
                productId,
                variantId,
                reviewCount,
                ratingCount,
                Math.round(averageRating * 10.0) / 10.0,
                fiveStarCount,
                fourStarCount,
                threeStarCount,
                twoStarCount,
                oneStarCount
        );
    }

    private ProductReviewDTO toDto(ProductReview review) {
        User user = review.getUser();
        Product product = review.getProduct();
        ProductVariant variant = review.getProductVariant();
        return new ProductReviewDTO(
                review.getId(),
                review.getOrder().getId(),
                review.getOrderItem().getId(),
                product.getId(),
                variant.getId(),
                product.getProductName(),
                variant.getVariantName(),
                review.getRating(),
                review.getComment(),
                deserializeImageUrls(review.getImageUrls()),
                user.getId(),
                user.getUsername(),
                buildDisplayName(user),
                user.getAvatarUrl(),
                review.getStatus(),
                true,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        return imageUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(MAX_IMAGE_COUNT)
                .toList();
    }

    private String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, ConstantErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    private List<String> deserializeImageUrls(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(imageUrls, STRING_LIST);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String buildDisplayName(User user) {
        String fullName = ((user.getLastname() == null ? "" : user.getLastname()) + " "
                + (user.getFirstname() == null ? "" : user.getFirstname())).trim();
        return StringUtils.hasText(fullName) ? fullName : user.getUsername();
    }

    private Pageable normalizePageable(Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 20 : pageable.getPageSize();
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }
}
