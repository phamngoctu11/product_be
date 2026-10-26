package com.example.workflow.repository;

import com.example.workflow.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {
    List<ProductType> findAllByActiveTrueOrderByNameAsc();
    List<ProductType> findAllByOrderByNameAsc();
    Optional<ProductType> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
