package com.example.workflow.repository;

import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.nume.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.productVariant pv " +
            "LEFT JOIN FETCH pv.product " +
            "LEFT JOIN FETCH o.userVoucher uv " +
            "LEFT JOIN FETCH uv.template " +
            "WHERE o.id = :id")
    Optional<Order> findById(@Param("id") Long id);

    // ==============================================================
    // 2. MASTER/LIST VIEW (Dùng DTO Projection & JOIN thường)
    // ==============================================================

    @Query(value = "SELECT o.id " +
            "FROM Order o JOIN o.user u " +
            "WHERE u.id = :userId " +
            "ORDER BY " +
            "CASE " +
            "WHEN o.status IN :oldestFirstStatuses THEN 0 " +
            "WHEN o.status = :shippingStatus THEN 1 " +
            "WHEN o.status = :deliveredStatus THEN 2 " +
            "WHEN o.status = :cancelledStatus THEN 3 " +
            "ELSE 4 END ASC, " +
            "CASE WHEN o.status IN :oldestFirstStatuses THEN o.startOrderTime ELSE NULL END ASC, " +
            "CASE WHEN o.status = :deliveredStatus THEN o.endOrderTime ELSE NULL END DESC, " +
            "o.startOrderTime DESC",
            countQuery = "SELECT COUNT(o) FROM Order o JOIN o.user u WHERE u.id = :userId")
    Page<Long> findOrderIdsByUserId(
            @Param("userId") Long userId,
            @Param("oldestFirstStatuses") List<OrderStatus> oldestFirstStatuses,
            @Param("shippingStatus") OrderStatus shippingStatus,
            @Param("deliveredStatus") OrderStatus deliveredStatus,
            @Param("cancelledStatus") OrderStatus cancelledStatus,
            Pageable pageable
    );

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.productVariant pv " +
            "LEFT JOIN FETCH pv.product " +
            "LEFT JOIN FETCH o.userVoucher uv " +
            "LEFT JOIN FETCH uv.template " +
            "WHERE o.id IN :orderIds")
    List<Order> findFullOrdersByIds(@Param("orderIds") List<Long> orderIds);

    @Query(value = "SELECT new com.example.workflow.dto.OrderListDTO(o.id, u.lastname, o.totalPrice, o.status, o.startOrderTime, o.paymentMethod) " +
            "FROM Order o JOIN o.user u " +
            "WHERE o.status = :status " +
            "ORDER BY o.startOrderTime ASC",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Page<OrderListDTO> findListDtoByStatusOldestFirst(@Param("status") OrderStatus status, Pageable pageable);

    @Query(value = "SELECT new com.example.workflow.dto.OrderListDTO(o.id, u.lastname, o.totalPrice, o.status, o.startOrderTime, o.paymentMethod) " +
            "FROM Order o JOIN o.user u " +
            "WHERE o.status = :status AND o.warehouseStaff IS NULL " +
            "ORDER BY o.startOrderTime ASC",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.warehouseStaff IS NULL")
    Page<OrderListDTO> findUnassignedListDtoByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Query(value = "SELECT new com.example.workflow.dto.OrderListDTO(o.id, u.lastname, o.totalPrice, o.status, o.startOrderTime, o.paymentMethod) " +
            "FROM Order o JOIN o.user u JOIN o.warehouseStaff staff " +
            "WHERE staff.id = :staffId AND o.status IN :statuses " +
            "ORDER BY " +
            "CASE " +
            "WHEN o.status = :assignedStatus THEN 0 " +
            "WHEN o.status = :kcsStatus THEN 1 " +
            "WHEN o.status = :shippingStatus THEN 2 " +
            "ELSE 3 END ASC, " +
            "CASE WHEN o.status IN :oldestFirstStatuses THEN o.startOrderTime ELSE NULL END ASC, " +
            "o.startOrderTime DESC",
            countQuery = "SELECT COUNT(o) FROM Order o JOIN o.warehouseStaff staff WHERE staff.id = :staffId AND o.status IN :statuses")
    Page<OrderListDTO> findListDtoByWarehouseStaffIdAndStatusIn(
            @Param("staffId") Long staffId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("oldestFirstStatuses") List<OrderStatus> oldestFirstStatuses,
            @Param("assignedStatus") OrderStatus assignedStatus,
            @Param("kcsStatus") OrderStatus kcsStatus,
            @Param("shippingStatus") OrderStatus shippingStatus,
            Pageable pageable
    );

    // ==============================================================
    // 3. THỐNG KÊ
    // ==============================================================

    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = :status")
    Double calculateTotalRevenue(@Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countOrdersByStatus(@Param("status") OrderStatus status);
}
