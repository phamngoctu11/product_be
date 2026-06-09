package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "variant_name", nullable = false)
    private String variantName;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;

    @Column(name = "image_url")
    private String imageUrl;
    @Column(name = "is_delete", nullable = false)
    private boolean isDelete = false;
}
