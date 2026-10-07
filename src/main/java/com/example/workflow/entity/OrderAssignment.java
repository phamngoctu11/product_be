package com.example.workflow.entity;

import com.example.workflow.nume.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** Domain contract for the new order lifecycle; endpoints are implemented in later phases. */
@Entity
@org.hibernate.annotations.Check(constraints = "(status = 'ACTIVE' AND active_order_id IS NOT NULL AND active_staff_id IS NOT NULL AND active_order_id = order_id AND active_staff_id = staff_id AND released_at IS NULL AND release_reason IS NULL) OR (status = 'RELEASED' AND active_order_id IS NULL AND active_staff_id IS NULL AND released_at IS NOT NULL AND release_reason IS NOT NULL AND release_reason IN ('FINAL_KCS_PASSED', 'ORDER_CANCELLED'))")
@Getter
@Setter
@Table(name = "order_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_assignments_1", columnNames = {"active_order_id"}),
        @UniqueConstraint(name = "uk_order_assignments_2", columnNames = {"active_staff_id"})
})
public class OrderAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "order_id", nullable = false, columnDefinition = "bigint")
    private Long orderId;

    @Column(name = "staff_id", nullable = false, columnDefinition = "varchar(36)")
    private String staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, columnDefinition = "varchar(24)")
    private AssignmentSource source;

    @Column(name = "assigned_by", nullable = true, columnDefinition = "varchar(36)")
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(24)")
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "released_at", nullable = true, columnDefinition = "datetime")
    private LocalDateTime releasedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_reason", nullable = true, columnDefinition = "varchar(32)")
    private AssignmentReleaseReason releaseReason;

    @Column(name = "active_order_id", nullable = true, columnDefinition = "bigint")
    private Long activeOrderId;

    @Column(name = "active_staff_id", nullable = true, columnDefinition = "varchar(36)")
    private String activeStaffId;

    public void occupy(Long orderId, String staffId, AssignmentSource source, String actorId, LocalDateTime at) {
        if (this.id != null || this.orderId != null) throw new IllegalStateException("Assignment already initialized");
        this.orderId = java.util.Objects.requireNonNull(orderId);
        this.staffId = java.util.Objects.requireNonNull(staffId);
        this.source = java.util.Objects.requireNonNull(source);
        this.assignedBy = actorId;
        this.assignedAt = java.util.Objects.requireNonNull(at);
        this.activeOrderId = orderId;
        this.activeStaffId = staffId;
        this.status = AssignmentStatus.ACTIVE;
    }

    public void release(AssignmentReleaseReason reason, LocalDateTime at) {
        if (status == AssignmentStatus.RELEASED) return;
        if (orderId == null || staffId == null) throw new IllegalStateException("Assignment not initialized");
        this.releaseReason = java.util.Objects.requireNonNull(reason);
        this.releasedAt = java.util.Objects.requireNonNull(at);
        this.activeOrderId = null;
        this.activeStaffId = null;
        this.status = AssignmentStatus.RELEASED;
    }
}
