package com.example.workflow.repository;

import com.example.workflow.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
