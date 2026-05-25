package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name="order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id", referencedColumnName = "id")
    @ToString.Exclude
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name="price")
    private double price;

    // ==========================================
    // 🚨 QUY TRÌNH ĐỐI SOÁT 3 ĐIỂM CHẠM
    // ==========================================

    @Column(name="quantity")
    private int quantity; // BƯỚC 1: Số lượng khách đặt trên web (KHÔNG ĐỔI)

    @Column(name="exported_quantity")
    private Integer exportedQuantity; // BƯỚC 2: Số lượng thực tế nhân viên nhặt xuất kho (Ban đầu là null)

    @Column(name="received_quantity")
    private Integer receivedQuantity; // BƯỚC 3: Số lượng thực tế khách nhận được (Ban đầu là null)
}