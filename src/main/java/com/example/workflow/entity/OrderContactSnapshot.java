package com.example.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable-at-business-level contact data captured when an order is placed.
 *
 * <p>The snapshot is embedded in {@link Order} so the existing order columns and
 * data remain compatible while the domain model makes the snapshot explicit.</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderContactSnapshot {

    @Column(name = "recipient_name", length = 120)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "recipient_phone", length = 30)
    private String phone;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
