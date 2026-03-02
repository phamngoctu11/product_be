package com.example.workflow.repository;

import com.example.workflow.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. Dùng Database level để trừ kho. Nếu kho < amount sẽ không update được (trả về 0)
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :amount WHERE p.id = :id AND p.quantity >= :amount")
    int decreaseStockIfAvailable(@Param("id") Long id, @Param("amount") int amount);

    // 2. Thêm hàm hỗ trợ Phân trang
    Page<Product> findAll(Pageable pageable);
}