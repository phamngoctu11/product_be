package com.example.workflow.entity;

import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.GuestWorkflowStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.nume.ManagerReviewDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    private double totalPrice;
    private LocalDateTime startOrderTime;
    private LocalDateTime endOrderTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, columnDefinition = "varchar(50)")
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20, columnDefinition = "varchar(20)")
    private OrderType orderType = OrderType.CATALOG;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20, columnDefinition = "varchar(20)")
    // Null is retained for legacy rows whose payment outcome has not been reconciled.
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "production_status",
            nullable = false,
            length = 32,
            columnDefinition = "varchar(32) default 'NOT_REQUIRED'"
    )
    private OrderProductionStatus productionStatus = OrderProductionStatus.NOT_REQUIRED;

    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id")
    private UserVoucher userVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_voucher_template_id")
    private VoucherTemplate guestVoucherTemplate;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "final_price")
    private Double finalPrice;

    @Column(name = "payment_method")
    private String paymentMethod;

    /**
     * Normalized payment method used by the new lifecycle. The legacy string
     * remains readable during migration and will be removed after consumers
     * have switched.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_v2", length = 20, columnDefinition = "varchar(20)")
    private PaymentMethod paymentMethodType;

    @Column(name = "guest_session_id", length = 128)
    private String guestSessionId;

    @Embedded
    private OrderContactSnapshot contactSnapshot;

    /**
     * SHA-256 hash of the public lookup token. The raw token is returned once at
     * checkout time and must never be persisted.
     */
    @Column(name = "order_lookup_token_hash", length = 64, unique = true)
    private String orderLookupTokenHash;

    @Column(name = "order_lookup_token_created_at")
    private LocalDateTime orderLookupTokenCreatedAt;

    @Column(name = "order_lookup_token_scope", length = 100)
    private String orderLookupTokenScope;

    @Column(name = "order_lookup_token_expires_at")
    private LocalDateTime orderLookupTokenExpiresAt;

    @Column(name = "order_lookup_token_revoked_at")
    private LocalDateTime orderLookupTokenRevokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "guest_workflow_status", length = 32)
    private GuestWorkflowStatus guestWorkflowStatus;

    @Column(name = "guest_workflow_process_instance_id", length = 64)
    private String guestWorkflowProcessInstanceId;

    @Column(name = "guest_workflow_started_at")
    private LocalDateTime guestWorkflowStartedAt;

    @Column(name = "guest_workflow_error", length = 1000)
    private String guestWorkflowError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "approved_by_id")
    private String approvedById;

    @Column(name = "approved_by_full_name")
    private String approvedByFullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_staff_id")
    private User warehouseStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @Column(name = "manager_approved_at")
    private LocalDateTime managerApprovedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_review_decision", length = 16, columnDefinition = "varchar(16)")
    private ManagerReviewDecision managerReviewDecision;

    @Column(name = "manager_rejected_at")
    private LocalDateTime managerRejectedAt;

    @Column(name = "confirmation_due_at")
    private LocalDateTime confirmationDueAt;

    @Column(name = "user_submitted_at")
    private LocalDateTime userSubmittedAt;

    @Column(name = "order_accepted_at")
    private LocalDateTime orderAcceptedAt;

    @Column(name = "production_started_at")
    private LocalDateTime productionStartedAt;

    @Column(name = "ready_to_ship_at")
    private LocalDateTime readyToShipAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "planned_start_at")
    private LocalDateTime plannedStartAt;

    @Column(name = "late_start_reason", length = 1000)
    private String lateStartReason;

    @Column(name = "current_agreement_version")
    private Integer currentAgreementVersion;

    @Column(name = "expected_shipping_days", nullable = false)
    private int expectedShippingDays = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_source", length = 50, columnDefinition = "varchar(50)")
    private CancellationSource cancellationSource;

    @Column(name = "cancellation_reference", length = 100)
    private String cancellationReference;

    @Column(name = "shipping_provider")
    private String shippingProvider;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Column(name = "stock_reserved", nullable = false, columnDefinition = "boolean default false")
    private boolean stockReserved = false;

    @Column(name = "stock_deducted", nullable = false, columnDefinition = "boolean default false")
    private boolean stockDeducted = false;

    public String getNote() {
        return contactSnapshot == null ? null : contactSnapshot.getNote();
    }

    public void setNote(String note) {
        ensureContactSnapshot().setNote(note);
    }

    public String getEmail() {
        return contactSnapshot == null ? null : contactSnapshot.getEmail();
    }

    public void setEmail(String email) {
        ensureContactSnapshot().setEmail(email);
    }

    public String getRecipientName() {
        return contactSnapshot == null ? null : contactSnapshot.getFullName();
    }

    public void setRecipientName(String recipientName) {
        ensureContactSnapshot().setFullName(recipientName);
    }

    public String getRecipientPhone() {
        return contactSnapshot == null ? null : contactSnapshot.getPhone();
    }

    public void setRecipientPhone(String recipientPhone) {
        ensureContactSnapshot().setPhone(recipientPhone);
    }

    public String getShippingAddress() {
        return contactSnapshot == null ? null : contactSnapshot.getAddress();
    }

    public void setShippingAddress(String shippingAddress) {
        ensureContactSnapshot().setAddress(shippingAddress);
    }

    private OrderContactSnapshot ensureContactSnapshot() {
        if (contactSnapshot == null) {
            contactSnapshot = new OrderContactSnapshot();
        }
        return contactSnapshot;
    }
}
