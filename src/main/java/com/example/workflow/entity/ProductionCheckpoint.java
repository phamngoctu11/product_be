package com.example.workflow.entity;

import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionCheckpointStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "production_checkpoints",
        indexes = {
                @Index(name = "idx_checkpoint_item_stage", columnList = "order_item_id, stage"),
                @Index(name = "idx_checkpoint_status", columnList = "status, submitted_at"),
                @Index(name = "idx_checkpoint_submitter", columnList = "submitted_by_id, submitted_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_checkpoint_item_stage_attempt",
                        columnNames = {"order_item_id", "stage", "attempt_number"}
                )
        }
)
public class ProductionCheckpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    @ToString.Exclude
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    private ProductionCheckpointStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ProductionCheckpointStatus status = ProductionCheckpointStatus.SUBMITTED;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "manager_review_required", nullable = false, columnDefinition = "boolean default true")
    private boolean managerReviewRequired = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private User submittedBy;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "checkpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ProductionCheckpointImage> images = new ArrayList<>();

    @OneToOne(mappedBy = "checkpoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ProductionDecision decision;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (attemptNumber < 1) {
            throw new IllegalStateException("Checkpoint attempt number must be at least 1");
        }
    }

    public void addImage(ProductionCheckpointImage image) {
        if (image == null) {
            return;
        }
        images.add(image);
        image.setCheckpoint(this);
    }

    public void attachDecision(ProductionDecision productionDecision) {
        decision = productionDecision;
        if (productionDecision != null) {
            productionDecision.setCheckpoint(this);
        }
    }
}
