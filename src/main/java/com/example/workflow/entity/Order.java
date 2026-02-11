package com.example.workflow.entity;
import com.example.workflow.nume.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
@Data
@Entity
@NoArgsConstructor
@Table(name="orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderItem> items;
    double totalPrice;
    LocalDateTime startOrderTime;
    LocalDateTime endOrderTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    OrderStatus status;
    String cancelReason;
}
