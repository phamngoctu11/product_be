package com.example.workflow.entity;

import com.example.workflow.nume.ProductAvailabilityStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

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

    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @DecimalMin(value = "2.0", message = "Made day must be at least 2")
    @Column(name = "made_day")
    private Double madeDay;

    @Column(name = "tags")
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id")
    private ProductType productType;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(name = "image_url")
    private String imageUrl;

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
