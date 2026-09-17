package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "guest_voucher_usages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_guest_voucher_usage_order", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_guest_voucher_usage_session", columnNames = {"voucher_template_id", "guest_session_id"}),
                @UniqueConstraint(name = "uk_guest_voucher_usage_email", columnNames = {"voucher_template_id", "email_hash"}),
                @UniqueConstraint(name = "uk_guest_voucher_usage_phone", columnNames = {"voucher_template_id", "phone_hash"})
        }
)
public class GuestVoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_template_id", nullable = false)
    private VoucherTemplate voucherTemplate;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "guest_session_id", nullable = false, length = 128)
    private String guestSessionId;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "phone_hash", nullable = false, length = 64)
    private String phoneHash;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;
}
