package com.example.workflow.entity;

import com.example.workflow.nume.OrderStatus;
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

    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id")
    private UserVoucher userVoucher;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "final_price")
    private Double finalPrice = 0.0;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "email")
    private String email;

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
}
