package com.example.workflow.repository;

import com.example.workflow.dto.BestSellerProductDTO;
import com.example.workflow.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    @Modifying
    @Query("UPDATE InventoryTransaction tx SET tx.transactionType = :targetType " +
            "WHERE tx.order.id = :orderId AND tx.transactionType = :sourceType")
    int updateTypeByOrderId(
            @Param("orderId") Long orderId,
            @Param("sourceType") String sourceType,
            @Param("targetType") String targetType
    );

    @Query(value = "SELECT new com.example.workflow.dto.BestSellerProductDTO(" +
            "p.id, p.productName, SUM(ABS(tx.quantityChange)), p.imageUrl) " +
            "FROM InventoryTransaction tx " +
            "JOIN tx.productVariant v " +
            "JOIN v.product p " +
            "WHERE tx.transactionType = 'SALE' " +
            "AND tx.quantityChange < 0 " +
            "AND tx.createdAt >= :fromTime " +
            "AND tx.createdAt < :toTime " +
            "AND p.isDelete = false " +
            "GROUP BY p.id, p.productName, p.imageUrl " +
            "ORDER BY SUM(ABS(tx.quantityChange)) DESC, p.id DESC",
            countQuery = "SELECT COUNT(DISTINCT p.id) " +
                    "FROM InventoryTransaction tx " +
                    "JOIN tx.productVariant v " +
                    "JOIN v.product p " +
                    "WHERE tx.transactionType = 'SALE' " +
                    "AND tx.quantityChange < 0 " +
                    "AND tx.createdAt >= :fromTime " +
                    "AND tx.createdAt < :toTime " +
                    "AND p.isDelete = false")
    Page<BestSellerProductDTO> findBestSellingProducts(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable
    );
}
