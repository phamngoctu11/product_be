package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name="order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    @ManyToOne
    @JoinColumn(name="order_id",referencedColumnName = "id")
    @ToString.Exclude
    private Order order;
    @ManyToOne
    @JoinColumn(name="product_id",referencedColumnName = "id")
    private Product product;
    @Column(name="quantity")
    private int quantity;
    @Column(name="price")
    private double price;
}
