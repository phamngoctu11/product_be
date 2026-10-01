package com.example.workflow.entity;

import com.example.workflow.nume.ProductAvailabilityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "price")
    private double price;

    @Column(name = "tags")
    private String tags;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_handmade", nullable = false, columnDefinition = "boolean default true")
    private boolean handmade = true;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "availability_status",
            nullable = false,
            length = 32,
            columnDefinition = "varchar(32) default 'ACCEPTING_ORDERS'"
    )
    private ProductAvailabilityStatus availabilityStatus = ProductAvailabilityStatus.ACCEPTING_ORDERS;

    private boolean isDelete;
}
