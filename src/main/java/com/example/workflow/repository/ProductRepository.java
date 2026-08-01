package com.example.workflow.repository;

import com.example.workflow.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = "SELECT p FROM Product p " +
            "LEFT JOIN p.variants v ON v.isDelete = false " +
            "WHERE p.isDelete = false " +
            "GROUP BY p " +
            "ORDER BY CASE WHEN SUM(COALESCE(v.quantity, 0)) > 0 THEN 1 ELSE 0 END DESC, p.id DESC",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isDelete = false")
    Page<Product> findAllByStockPriority(Pageable pageable);

    @Query(value = "SELECT p FROM Product p " +
            "LEFT JOIN p.variants v ON v.isDelete = false " +
            "WHERE p.isDelete = false " +
            "AND (:keyword IS NULL OR LOWER(p.productName) LIKE CONCAT('%', :keyword, '%') OR LOWER(p.tags) LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "GROUP BY p " +
            "ORDER BY CASE WHEN SUM(COALESCE(v.quantity, 0)) > 0 THEN 1 ELSE 0 END DESC, p.id DESC",
            countQuery = "SELECT COUNT(p) FROM Product p " +
                    "WHERE p.isDelete = false " +
                    "AND (:keyword IS NULL OR LOWER(p.productName) LIKE CONCAT('%', :keyword, '%') OR LOWER(p.tags) LIKE CONCAT('%', :keyword, '%')) " +
                    "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                    "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchByStockPriority(
            @Param("keyword") String keyword,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );

}
