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
@Table(name = "order_receipts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_receipts_1", columnNames = {"order_id"})
})
public class OrderReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "order_id", nullable = false, columnDefinition = "bigint")
    private Long orderId;

    @Column(name = "confirmed_by", nullable = true, columnDefinition = "varchar(36)")
    private String confirmedBy;

    @Column(name = "guest_token_reference", nullable = true, columnDefinition = "varchar(64)")
    private String guestTokenReference;

    @Column(name = "confirmed_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime confirmedAt;

    @Column(name = "mismatch_accepted", nullable = false, columnDefinition = "boolean")
    private boolean mismatchAccepted = false;

    @Column(name = "items_snapshot", nullable = false, columnDefinition = "text")
    private String itemsSnapshot;
}
