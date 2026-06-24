package com.example.workflow.entity;

import com.example.workflow.nume.ConsultationAttributionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "consultation_sale_attributions",
        indexes = {
                @Index(name = "idx_consultation_attr_order", columnList = "order_id"),
                @Index(name = "idx_consultation_attr_staff_status", columnList = "staff_id, status, created_at"),
                @Index(name = "idx_consultation_attr_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_consultation_attr_product_created", columnList = "product_id, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_consultation_attribution_order_item", columnNames = "order_item_id")
        }
)
public class ConsultationSaleAttribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_request_id", nullable = false)
    private ConsultationRequest consultationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "consultation_created_at", nullable = false)
    private LocalDateTime consultationCreatedAt;

    @Column(name = "order_created_at", nullable = false)
    private LocalDateTime orderCreatedAt;

    @Column(name = "minutes_from_consultation_to_order", nullable = false)
    private long minutesFromConsultationToOrder;

    @Column(name = "item_amount", nullable = false)
    private double itemAmount;

    @Column(name = "bonus_eligible", nullable = false)
    private boolean bonusEligible = true;

    @Column(name = "bonus_percent")
    private Double bonusPercent;

    @Column(name = "bonus_amount")
    private Double bonusAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ConsultationAttributionStatus status = ConsultationAttributionStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
