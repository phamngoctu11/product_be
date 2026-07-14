package com.example.workflow.repository;

import com.example.workflow.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT v FROM ProductVariant v " +
            "JOIN FETCH v.product p " +
            "WHERE v.id = :variantId " +
            "AND v.isDelete = false " +
            "AND p.isDelete = false")
    Optional<ProductVariant> findActiveById(@Param("variantId") Long variantId);

    // Hàm trừ kho chuẩn xác cho Biến thể
    @Modifying
    @Query("UPDATE ProductVariant v SET v.quantity = v.quantity - :amount WHERE v.id = :variantId AND v.quantity >= :amount")
    int decreaseStockIfAvailable(@Param("variantId") Long variantId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.quantity = v.quantity + :amount WHERE v.id = :variantId")
    int increaseStock(@Param("variantId") Long variantId, @Param("amount") int amount);

    @Query("SELECT v.quantity FROM ProductVariant v WHERE v.id = :variantId")
    Optional<Integer> findStockById(@Param("variantId") Long variantId);
}
