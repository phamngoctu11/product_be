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
@Table(name = "change_requests")
public class ChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "order_id", nullable = false, columnDefinition = "bigint")
    private Long orderId;

    @Column(name = "owner_id", nullable = false, columnDefinition = "varchar(36)")
    private String ownerId;

    @Column(name = "base_agreement_version", nullable = false, columnDefinition = "int")
    private Integer baseAgreementVersion;

    @Column(name = "requested_details", nullable = false, columnDefinition = "text")
    private String requestedDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(24)")
    private ChangeRequestStatus status = ChangeRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "decided_by", nullable = true, columnDefinition = "varchar(36)")
    private String decidedBy;

    @Column(name = "decided_at", nullable = true, columnDefinition = "datetime")
    private LocalDateTime decidedAt;

    @Column(name = "decision_reason", nullable = true, columnDefinition = "text")
    private String decisionReason;

    @Column(name = "applied_version", nullable = true, columnDefinition = "int")
    private Integer appliedVersion;
}
