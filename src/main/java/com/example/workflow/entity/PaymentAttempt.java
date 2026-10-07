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
@Table(name = "payment_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_attempts_1", columnNames = {"provider", "request_reference"}),
        @UniqueConstraint(name = "uk_payment_attempts_2", columnNames = {"provider", "provider_transaction_id"})
})
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "order_id", nullable = false, columnDefinition = "bigint")
    private Long orderId;

    @Column(name = "provider", nullable = false, columnDefinition = "varchar(32)")
    private String provider;

    @Column(name = "request_reference", nullable = false, columnDefinition = "varchar(128)")
    private String requestReference;

    @Column(name = "provider_transaction_id", nullable = true, columnDefinition = "varchar(128)")
    private String providerTransactionId;

    @Column(name = "expected_amount", nullable = false, columnDefinition = "double")
    private Double expectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(24)")
    private PaymentAttemptStatus status = PaymentAttemptStatus.PENDING;

    @Column(name = "opened_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime openedAt;

    @Column(name = "due_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime dueAt;

    @Column(name = "paid_at", nullable = true, columnDefinition = "datetime")
    private LocalDateTime paidAt;

    @Column(name = "provider_result", nullable = true, columnDefinition = "text")
    private String providerResult;

    @Column(name = "reconciliation_required", nullable = false, columnDefinition = "boolean")
    private boolean reconciliationRequired = false;
}
