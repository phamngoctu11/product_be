package com.example.workflow.repository;

import com.example.workflow.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {
    List<ProductType> findAllByActiveTrueOrderByNameAsc();
    List<ProductType> findAllByOrderByNameAsc();
    Optional<ProductType> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT DISTINCT pt FROM ProductType pt " +
            "WHERE pt.active = true AND (LOWER(pt.name) LIKE :pattern OR LOWER(pt.code) LIKE :pattern " +
            "OR pt.id IN (SELECT p.productType.id FROM Product p WHERE p.isDelete = false " +
            "AND p.productType IS NOT NULL AND (LOWER(p.productName) LIKE :pattern OR LOWER(p.tags) LIKE :pattern))) " +
            "ORDER BY pt.name ASC")
    List<ProductType> findSearchSuggestions(@Param("pattern") String pattern, Pageable pageable);
}
