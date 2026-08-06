package com.example.workflow.repository;

import com.example.workflow.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi FROM OrderItem oi " +
            "JOIN FETCH oi.order o " +
            "JOIN FETCH o.user " +
            "JOIN FETCH oi.productVariant v " +
            "JOIN FETCH v.product " +
            "WHERE oi.id = :id")
    Optional<OrderItem> findReviewTargetById(@Param("id") Long id);
}
