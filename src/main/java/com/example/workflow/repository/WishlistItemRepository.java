package com.example.workflow.repository;

import com.example.workflow.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    @Query(
            value = "SELECT wi FROM WishlistItem wi JOIN FETCH wi.product p " +
                    "WHERE wi.user.id = :userId AND p.isDelete = false " +
                    "ORDER BY wi.createdAt DESC",
            countQuery = "SELECT COUNT(wi) FROM WishlistItem wi JOIN wi.product p " +
                    "WHERE wi.user.id = :userId AND p.isDelete = false"
    )
    Page<WishlistItem> findActiveByUserId(@Param("userId") String userId, Pageable pageable);

    Optional<WishlistItem> findByUser_IdAndProduct_Id(String userId, Long productId);

    @Query("SELECT wi.product.id FROM WishlistItem wi JOIN wi.product p " +
            "WHERE wi.user.id = :userId " +
            "AND p.id IN :productIds " +
            "AND p.isDelete = false")
    Set<Long> findExistingProductIdsInWishlist(
            @Param("userId") String userId,
            @Param("productIds") Collection<Long> productIds
    );

    void deleteByUser_IdAndProduct_Id(String userId, Long productId);
}
