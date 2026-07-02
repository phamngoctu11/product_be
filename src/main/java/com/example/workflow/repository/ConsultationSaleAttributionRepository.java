package com.example.workflow.repository;

import com.example.workflow.entity.ConsultationSaleAttribution;
import com.example.workflow.nume.ConsultationAttributionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConsultationSaleAttributionRepository extends JpaRepository<ConsultationSaleAttribution, Long> {
    boolean existsByOrderItemId(Long orderItemId);

    @Query("SELECT a.orderItem.id FROM ConsultationSaleAttribution a WHERE a.orderItem.id IN :orderItemIds")
    List<Long> findExistingOrderItemIds(@Param("orderItemIds") Collection<Long> orderItemIds);

    List<ConsultationSaleAttribution> findByOrderId(Long orderId);

    Page<ConsultationSaleAttribution> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<ConsultationSaleAttribution> findByStaffIdAndStatusInOrderByCreatedAtDesc(
            String staffId,
            Collection<ConsultationAttributionStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "consultationRequest",
            "order",
            "orderItem",
            "user",
            "staff",
            "product",
            "productVariant"
    })
    Page<ConsultationSaleAttribution> findByStaffIdAndStatusAndConfirmedAtGreaterThanEqualAndConfirmedAtLessThanOrderByConfirmedAtDesc(
            String staffId,
            ConsultationAttributionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "consultationRequest",
            "order",
            "orderItem",
            "user",
            "staff",
            "product",
            "productVariant"
    })
    Page<ConsultationSaleAttribution> findByStaffIdAndStatusAndOrderCreatedAtGreaterThanEqualAndOrderCreatedAtLessThanOrderByOrderCreatedAtDesc(
            String staffId,
            ConsultationAttributionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "consultationRequest",
            "order",
            "orderItem",
            "user",
            "staff",
            "product",
            "productVariant"
    })
    Page<ConsultationSaleAttribution> findByStaffIdAndStatusAndCancelledAtGreaterThanEqualAndCancelledAtLessThanOrderByCancelledAtDesc(
            String staffId,
            ConsultationAttributionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"order", "staff"})
    List<ConsultationSaleAttribution> findByStaffIdAndStatusAndConfirmedAtGreaterThanEqualAndConfirmedAtLessThan(
            String staffId,
            ConsultationAttributionStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {"order", "staff"})
    List<ConsultationSaleAttribution> findByStaffIdAndStatusAndOrderCreatedAtGreaterThanEqualAndOrderCreatedAtLessThan(
            String staffId,
            ConsultationAttributionStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {"order", "staff"})
    List<ConsultationSaleAttribution> findByStaffIdAndStatusAndCancelledAtGreaterThanEqualAndCancelledAtLessThan(
            String staffId,
            ConsultationAttributionStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<ConsultationSaleAttribution> findByIdAndUserId(Long id, String userId);
}
