package com.example.workflow.entity;

import com.example.workflow.nume.ProductionDecisionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "production_decisions",
        indexes = {
                @Index(name = "idx_production_decision_actor", columnList = "decided_by_id, decided_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_production_decision_checkpoint", columnNames = "checkpoint_id")
        }
)
public class ProductionDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkpoint_id", nullable = false)
    private ProductionCheckpoint checkpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 16)
    private ProductionDecisionType decisionType;

    @Column(name = "reason", length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decided_by_id", nullable = false)
    private User decidedBy;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private LocalDateTime decidedAt;

    @PrePersist
    void prePersist() {
        if (decidedAt == null) {
            decidedAt = LocalDateTime.now();
        }
    }
}
