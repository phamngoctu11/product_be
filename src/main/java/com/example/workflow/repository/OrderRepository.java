package com.example.workflow.repository;

import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.nume.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ==============================================================
    // 1. DETAIL VIEW (Dùng LEFT JOIN FETCH để lấy rập khuôn chi tiết)
    // ==============================================================
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.productVariant " +
            "WHERE o.id = :id")
    Optional<Order> findById(@Param("id") Long id);

    // ==============================================================
    // 2. MASTER/LIST VIEW (Dùng DTO Projection & JOIN thường)
    // ==============================================================

    @Query("SELECT new com.example.workflow.dto.OrderListDTO(o.id, u.lastname, o.totalPrice, o.status, o.startOrderTime, o.paymentMethod) " +
            "FROM Order o JOIN o.user u " +
            "WHERE u.id = :userId " +
            "ORDER BY o.startOrderTime DESC")
    List<OrderListDTO> findListDtoByUserId(@Param("userId") Long userId);

    @Query("SELECT new com.example.workflow.dto.OrderListDTO(o.id, u.lastname, o.totalPrice, o.status, o.startOrderTime, o.paymentMethod) " +
            "FROM Order o JOIN o.user u " +
            "WHERE o.status = :status " +
            "ORDER BY o.startOrderTime DESC")
    List<OrderListDTO> findListDtoByStatus(@Param("status") OrderStatus status);

    // ==============================================================
    // 3. THỐNG KÊ
    // ==============================================================

    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = :status")
    Double calculateTotalRevenue(@Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countOrdersByStatus(@Param("status") OrderStatus status);
}