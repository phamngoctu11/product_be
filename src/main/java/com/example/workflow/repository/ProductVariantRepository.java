package com.example.workflow.repository;

import com.example.workflow.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Hàm trừ kho chuẩn xác cho Biến thể
    @Modifying
    @Query("UPDATE ProductVariant v SET v.quantity = v.quantity - :amount WHERE v.id = :variantId AND v.quantity >= :amount")
    int decreaseStockIfAvailable(@Param("variantId") Long variantId, @Param("amount") int amount);
}