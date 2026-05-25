package com.example.workflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "voucher_templates")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Voucher code is required")
    @Column(unique = true, nullable = false)
    private String code;
    @NotBlank(message = "Voucher name is required")
    private String name;
    private String description;
    @Min(value = 0, message = "Point cost must be zero or positive")
    @Column(name = "point_cost")
    private int pointCost;
    @PositiveOrZero(message = "Minimum order value must be zero or positive")
    @Column(name = "min_order_value")
    private double minOrderValue;
    @PositiveOrZero(message = "Discount percent must be zero or positive")
    @Column(name = "discount_percent")
    private double discountPercent;
    @PositiveOrZero(message = "Maximum discount amount must be zero or positive")
    @Column(name = "max_discount_amount")
    private double maxDiscountAmount;
    @Min(value = 0, message = "Quantity must be zero or positive")
    @Column(name = "quantity")
    private int quantity;
    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private boolean isActive = true;
}
