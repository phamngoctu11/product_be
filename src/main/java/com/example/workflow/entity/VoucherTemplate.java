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
    @NotBlank(message = "Mã phiếu giảm giá là bắt buộc.")
    @Column(unique = true, nullable = false)
    private String code;
    @NotBlank(message = "Tên phiếu giảm giá là bắt buộc.")
    private String name;
    private String description;
    @Min(value = 0, message = "Số điểm cần để đổi phiếu phải là số không âm.")
    @Column(name = "point_cost")
    private int pointCost;
    @PositiveOrZero(message = "Giá trị đơn hàng tối thiểu phải là số không âm.")
    @Column(name = "min_order_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    @PositiveOrZero(message = "Phần trăm giảm giá phải là số không âm.")
    @Column(name = "discount_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal discountPercent = BigDecimal.ZERO;
    @PositiveOrZero(message = "Mức giảm giá tối đa phải là số không âm.")
    @Column(name = "max_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount = BigDecimal.ZERO;
    @Min(value = 0, message = "Số lượng phải là số không âm.")
    @Column(name = "quantity")
    private int quantity;
    @NotNull(message = "Ngày hết hạn là bắt buộc.")
    @Future(message = "Ngày hết hạn phải nằm trong tương lai.")
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_guest_voucher", nullable = false, columnDefinition = "boolean default false")
    private boolean guestVoucher = false;
}
