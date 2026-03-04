package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private VoucherTemplate template;

    @Column(name = "is_used")
    private boolean isUsed = false; // Đã xài chưa?

    @Column(name = "redeem_date")
    private LocalDateTime redeemDate; // Ngày đổi từ điểm ra voucher

    @Column(name = "used_date")
    private LocalDateTime usedDate; // Ngày thực sự áp dụng vào đơn hàng

    // THÊM TRƯỜNG NÀY: Ngày hết hạn cứng được copy từ Template sang
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
}