package com.example.workflow.repository;

import com.example.workflow.entity.Product;
import com.example.workflow.nume.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = "SELECT p FROM Product p " +
            "LEFT JOIN p.variants v " +
            "WHERE p.isDelete = false " +
            "GROUP BY p " +
            "ORDER BY CASE WHEN SUM(COALESCE(v.quantity, 0)) > 0 THEN 1 ELSE 0 END DESC, p.id DESC",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isDelete = false")
    Page<Product> findAllByStockPriority(Pageable pageable);

    @Query(value = "SELECT p FROM Product p " +
            "LEFT JOIN p.variants v " +
            "LEFT JOIN OrderItem oi ON oi.productVariant = v " +
            "LEFT JOIN oi.order o " +
            "WHERE p.isDelete = false " +
            "GROUP BY p " +
            "ORDER BY " +
            "COALESCE(SUM(CASE WHEN o.status = :deliveredStatus THEN oi.quantity ELSE 0 END), 0) DESC, " +
            "CASE WHEN SUM(COALESCE(v.quantity, 0)) > 0 THEN 1 ELSE 0 END DESC, " +
            "p.id DESC",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isDelete = false")
    Page<Product> findBestSellingProducts(@Param("deliveredStatus") OrderStatus deliveredStatus, Pageable pageable);
}
