package com.example.workflow.repository;

import com.example.workflow.dto.BestSellerProductDTO;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.nume.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi FROM OrderItem oi " +
            "JOIN FETCH oi.order o " +
            "JOIN FETCH o.user " +
            "JOIN FETCH oi.productVariant v " +
            "JOIN FETCH v.product " +
            "WHERE oi.id = :id")
    Optional<OrderItem> findReviewTargetById(@Param("id") Long id);

    @Query(value = "SELECT new com.example.workflow.dto.BestSellerProductDTO(" +
            "p.id, p.productName, SUM(oi.quantity), p.imageUrl) " +
            "FROM OrderItem oi JOIN oi.order o JOIN oi.productVariant v JOIN v.product p " +
            "WHERE o.status = :status AND o.deliveredAt >= :fromTime AND o.deliveredAt < :toTime " +
            "AND p.isDelete = false " +
            "GROUP BY p.id, p.productName, p.imageUrl " +
            "ORDER BY SUM(oi.quantity) DESC, p.id DESC",
            countQuery = "SELECT COUNT(DISTINCT p.id) FROM OrderItem oi " +
                    "JOIN oi.order o JOIN oi.productVariant v JOIN v.product p " +
                    "WHERE o.status = :status AND o.deliveredAt >= :fromTime AND o.deliveredAt < :toTime " +
                    "AND p.isDelete = false")
    Page<BestSellerProductDTO> findBestSellingProducts(
            @Param("status") OrderStatus status,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable
    );
}
