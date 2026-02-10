package com.example.workflow.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="cart_item")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    @ManyToOne
    @JoinColumn(name="cart_id",referencedColumnName = "id")
    private Cart cart;
    @ManyToOne
    @JoinColumn(name="product_id",referencedColumnName = "id")
    private Product product;
    @Column(name="quantity")
    private int quantity;
    @Column(name="price")
    private double price;
}
