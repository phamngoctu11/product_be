package com.example.workflow.entity;

import com.example.workflow.nume.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** Domain contract for the new order lifecycle; endpoints are implemented in later phases. */
@Entity
@Getter
@Setter
@Table(name = "order_agreements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_agreements_1", columnNames = {"order_id", "agreement_version"})
})
public class OrderAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "order_id", nullable = false, columnDefinition = "bigint")
    private Long orderId;

    @Column(name = "agreement_version", nullable = false, columnDefinition = "int")
    private Integer agreementVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(24)")
    private OrderAgreementStatus status = OrderAgreementStatus.DRAFT;

    @Column(name = "item_snapshot", nullable = false, columnDefinition = "text")
    private String itemSnapshot;

    @Column(name = "subtotal", nullable = true, columnDefinition = "double")
    private Double subtotal;

    @Column(name = "discount_amount", nullable = true, columnDefinition = "double")
    private Double discountAmount;

    @Column(name = "final_price", nullable = true, columnDefinition = "double")
    private Double finalPrice;

    @Column(name = "user_voucher_id", nullable = true, columnDefinition = "bigint")
    private Long userVoucherId;

    @Column(name = "submitted_by", nullable = true, columnDefinition = "varchar(36)")
    private String submittedBy;

    @Column(name = "submitted_at", nullable = true, columnDefinition = "datetime")
    private LocalDateTime submittedAt;

    @Column(name = "confirmed_by", nullable = true, columnDefinition = "varchar(36)")
    private String confirmedBy;

    @Column(name = "confirmed_at", nullable = true, columnDefinition = "datetime")
    private LocalDateTime confirmedAt;

    @Column(name = "decision_reason", nullable = true, columnDefinition = "text")
    private String decisionReason;
}
