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
@Table(name="orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderItem> items;

    double totalPrice;
    LocalDateTime startOrderTime;
    LocalDateTime endOrderTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",length = 50)
    OrderStatus status;

    String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_voucher_id")
    private UserVoucher userVoucher;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "final_price")
    private Double finalPrice = 0.0;

    @Column(name="payment_method")
    private String paymentMethod;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name="email")
    private String email;

    // ==========================================
    // 🚨 CÁC TRƯỜNG THÊM MỚI PHỤC VỤ ĐỐI SOÁT ERP
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager; // Chủ shop/Quản lý duyệt đơn hàng này

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_staff_id")
    private User warehouseStaff; // Thợ/Nhân viên kho trực tiếp đóng gói xuất kho

    @Column(name = "shipping_provider")
    private String shippingProvider; // Tên đơn vị vận chuyển (VD: GHTK, Viettel Post)

    @Column(name = "tracking_code")
    private String trackingCode; // Mã vận đơn
}