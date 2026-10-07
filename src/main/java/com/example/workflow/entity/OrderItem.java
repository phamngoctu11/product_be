package com.example.workflow.entity;

import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "order_item",
        indexes = {
                @Index(name = "idx_order_item_production", columnList = "order_id, production_status")
        }
)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id", referencedColumnName = "id")
    @ToString.Exclude
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name="price")
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20, columnDefinition = "varchar(20)")
    private OrderItemSourceType sourceType;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "variant_name_snapshot")
    private String variantNameSnapshot;

    @Column(name = "spec_snapshot", columnDefinition = "TEXT")
    private String specSnapshot;

    @Column(name = "made_day_snapshot")
    private Double madeDaySnapshot;

    @Column(name = "production_duration_days")
    private Integer productionDurationDays;

    @Column(name = "duration_rule_version", length = 32)
    private String durationRuleVersion;

    @Column(name = "computed_completion_at")
    private LocalDateTime computedCompletionAt;

    /**
     * Snapshot at checkout time. Product configuration may change after the
     * order was created, but its production route must remain stable.
     */
    @Column(name = "is_handmade", nullable = false, columnDefinition = "boolean default false")
    private boolean handmade = false;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "production_status",
            nullable = false,
            length = 40,
            columnDefinition = "varchar(40) default 'NOT_REQUIRED'"
    )
    private OrderItemProductionStatus productionStatus = OrderItemProductionStatus.NOT_REQUIRED;

    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OrderItemAssignment assignment;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("submittedAt ASC, id ASC")
    private List<ProductionCheckpoint> productionCheckpoints = new ArrayList<>();

    // ==========================================
    // 🚨 QUY TRÌNH ĐỐI SOÁT 3 ĐIỂM CHẠM
    // ==========================================

    @Column(name="quantity")
    private int quantity; // BƯỚC 1: Số lượng khách đặt trên web (KHÔNG ĐỔI)

    @Column(name="exported_quantity")
    private Integer exportedQuantity; // BƯỚC 2: Số lượng thực tế nhân viên nhặt xuất kho (Ban đầu là null)

    @Column(name="received_quantity")
    private Integer receivedQuantity; // BƯỚC 3: Số lượng thực tế khách nhận được (Ban đầu là null)
    public void attachAssignment(OrderItemAssignment orderItemAssignment) {
        assignment = orderItemAssignment;
        if (orderItemAssignment != null) {
            orderItemAssignment.setOrderItem(this);
        }
    }

    public void addProductionCheckpoint(ProductionCheckpoint checkpoint) {
        if (checkpoint == null) {
            return;
        }
        productionCheckpoints.add(checkpoint);
        checkpoint.setOrderItem(this);
    }
}
