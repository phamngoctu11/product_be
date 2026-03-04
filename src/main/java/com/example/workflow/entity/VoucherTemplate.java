package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Entity
@Table(name = "voucher_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String code;
    private String name;
    private String description;
    @Column(name = "point_cost")
    private int pointCost;
    @Column(name = "min_order_value")
    private double minOrderValue;
    @Column(name = "discount_percent")
    private double discountPercent;
    @Column(name = "max_discount_amount")
    private double maxDiscountAmount;
    @Column(name = "quantity")
    private int quantity;
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private boolean isActive = true;
}