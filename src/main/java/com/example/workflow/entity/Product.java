package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name="products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    // Giá này giờ chỉ đóng vai trò là "Giá hiển thị thấp nhất" ở ngoài danh mục
    @Column(name = "price")
    private double price;

    @Column(name = "tags")
    private String tags;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();
    @Column(name = "image_url")
    private String imageUrl;
    private boolean isDelete;
}