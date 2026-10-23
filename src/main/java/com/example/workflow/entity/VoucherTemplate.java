package com.example.workflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
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
    @Column(name = "min_order_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    @PositiveOrZero(message = "Discount percent must be zero or positive")
    @Column(name = "discount_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    @PositiveOrZero(message = "Maximum discount amount must be zero or positive")
    @Column(name = "max_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount = BigDecimal.ZERO;
    @Min(value = 0, message = "Quantity must be zero or positive")
    @Column(name = "quantity")
    private int quantity;
    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_guest_voucher", nullable = false, columnDefinition = "boolean default false")
    private boolean guestVoucher = false;
}
