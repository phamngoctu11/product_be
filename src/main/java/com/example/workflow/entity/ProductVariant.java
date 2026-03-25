package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

    // Tên biến thể cụ thể (VD: "Áo khoác da cá sấu", "Giày Sneaker Đen Size 42")
    @Column(name = "variant_name", nullable = false)
    private String variantName;

    // Giá bán cụ thể của biến thể này (Thay đổi tùy loại da/kích cỡ)
    @Column(name = "price", nullable = false)
    private double price;

    // Kho hàng riêng của biến thể này
    @Column(name = "quantity", nullable = false)
    private int quantity;

    // LƯU Ý HAY NHẤT: Lưu JSON các thuộc tính để cực kỳ linh hoạt (KHÔNG CẦN TẠO THÊM CỘT)
    // Ví dụ Quần áo: {"size": "XL", "color": "Đỏ"}
    // Ví dụ Gia dụng: {"material": "Nhựa", "capacity": "2 Lít"}
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;
    // Thêm vào Entity và DTO của Variant
    @Column(name = "image_url")
    private String imageUrl; // Trong DTO thì đặt là image_url cho khớp FE
}