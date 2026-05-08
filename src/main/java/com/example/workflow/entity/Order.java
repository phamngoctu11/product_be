package com.example.workflow.entity;
import com.example.workflow.nume.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
@Getter // Chỉ dùng Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
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
    private UserVoucher userVoucher; // Đơn này dùng voucher nào trong ví?
    @Column(name = "discount_amount")
    private Double discountAmount = 0.0; // Được giảm bao nhiêu tiền?
    @Column(name = "final_price")
    private Double finalPrice = 0.0;
    @Column(name="payment_method")
    private String paymentMethod;
    @Column(name = "note", columnDefinition = "TEXT")
    private String note; // Ghi chú của khách hàng
    @Column(name="email")
    private String email;
}
