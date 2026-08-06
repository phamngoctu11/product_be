package com.example.workflow.repository;

import com.example.workflow.entity.ProductReview;
import com.example.workflow.nume.ProductReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    boolean existsByOrderItem_Id(Long orderItemId);

    @EntityGraph(attributePaths = {"order", "orderItem", "user", "product", "productVariant"})
    Optional<ProductReview> findByIdAndUser_Id(Long id, String userId);

    @EntityGraph(attributePaths = {"order", "orderItem", "user", "product", "productVariant"})
    Page<ProductReview> findByProduct_IdAndStatusOrderByCreatedAtDesc(Long productId, ProductReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "orderItem", "user", "product", "productVariant"})
    Page<ProductReview> findByProductVariant_IdAndStatusOrderByCreatedAtDesc(Long variantId, ProductReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "orderItem", "user", "product", "productVariant"})
    Page<ProductReview> findByProductVariant_IdOrderByCreatedAtDesc(Long variantId, Pageable pageable);

    List<ProductReview> findByOrderItem_IdIn(Collection<Long> orderItemIds);

    long countByProduct_IdAndStatus(Long productId, ProductReviewStatus status);

    long countByProduct_IdAndStatusAndRatingIsNotNull(Long productId, ProductReviewStatus status);

    long countByProduct_IdAndStatusAndRating(Long productId, ProductReviewStatus status, Integer rating);

    long countByProductVariant_IdAndStatus(Long variantId, ProductReviewStatus status);

    long countByProductVariant_IdAndStatusAndRatingIsNotNull(Long variantId, ProductReviewStatus status);

    long countByProductVariant_IdAndStatusAndRating(Long variantId, ProductReviewStatus status, Integer rating);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r " +
            "WHERE r.product.id = :productId AND r.status = :status AND r.rating IS NOT NULL")
    double averageRatingByProduct(@Param("productId") Long productId, @Param("status") ProductReviewStatus status);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r " +
            "WHERE r.productVariant.id = :variantId AND r.status = :status AND r.rating IS NOT NULL")
    double averageRatingByVariant(@Param("variantId") Long variantId, @Param("status") ProductReviewStatus status);
}
