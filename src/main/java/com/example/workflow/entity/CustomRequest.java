package com.example.workflow.entity;

import com.example.workflow.nume.CustomRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "custom_requests",
        indexes = {
                @Index(
                        name = "idx_custom_request_owner_status_updated",
                        columnList = "owner_id,status,updated_at,id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_custom_requests_1", columnNames = "linked_order_id")
        }
)
public class CustomRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "owner_id", nullable = false, updatable = false, columnDefinition = "varchar(36)")
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(24)")
    private CustomRequestStatus status = CustomRequestStatus.DRAFT;

    @Column(name = "spec", nullable = false, columnDefinition = "text")
    private String spec;

    @Column(name = "attachments", nullable = true, columnDefinition = "text")
    private String attachments;

    @Column(name = "quantity", nullable = false, columnDefinition = "int")
    private Integer quantity;

    @Column(name = "linked_order_id", nullable = true, columnDefinition = "bigint")
    private Long linkedOrderId;

    @Column(name = "source_order_id", nullable = true, updatable = false, columnDefinition = "bigint")
    private Long sourceOrderId;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at", columnDefinition = "datetime")
    private LocalDateTime submittedAt;

    public boolean isEditable() {
        return status == CustomRequestStatus.DRAFT && linkedOrderId == null;
    }

    public void markSubmitted(Long orderId, LocalDateTime submissionTime) {
        if (!isEditable()) {
            throw new IllegalStateException("Only an unsubmitted draft can be linked to an order");
        }
        linkedOrderId = Objects.requireNonNull(orderId, "orderId must not be null");
        submittedAt = Objects.requireNonNull(submissionTime, "submissionTime must not be null");
        status = CustomRequestStatus.SUBMITTED;
        updatedAt = submissionTime;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = CustomRequestStatus.DRAFT;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
