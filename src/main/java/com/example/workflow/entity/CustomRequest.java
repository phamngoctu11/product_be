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
@Table(name = "custom_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_custom_requests_1", columnNames = {"linked_order_id"})
})
public class CustomRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "owner_id", nullable = false, columnDefinition = "varchar(36)")
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

    @Column(name = "source_order_id", nullable = true, columnDefinition = "bigint")
    private Long sourceOrderId;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;
}
