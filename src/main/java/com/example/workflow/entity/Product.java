package com.example.workflow.entity;
import jakarta.persistence.*;
import lombok.Data;
@Data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String product_name;
    private double price;
    private int quantity;
}