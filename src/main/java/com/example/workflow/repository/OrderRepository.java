package com.example.workflow.repository;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> getOrdersByUserId(Long user_id);
    Order getOrdersById(Long id);
    List<Order>findByStatus(OrderStatus status);
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'DELIVERED'")
    Double calculateTotalRevenue();

    // 2. Đếm số lượng đơn hàng theo trạng thái
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countOrdersByStatus(@Param("status") OrderStatus status);
}
