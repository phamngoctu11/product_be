package com.example.workflow.entity;

import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.GuestWorkflowStatus;
import com.example.workflow.nume.OrderProductionStatus;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    private double totalPrice;
    private LocalDateTime startOrderTime;
    private LocalDateTime endOrderTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private OrderStatus status;

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
    private Double finalPrice = 0.0;

    @Column(name = "payment_method")
    private String paymentMethod;

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
