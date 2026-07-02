package com.example.workflow.entity;

import com.example.workflow.nume.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="orderhistory")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name="oldstatus")
    OrderStatus oldstatus;

    @Enumerated(EnumType.STRING)
    @Column(name="newstatus")
    OrderStatus newstatus;

    @Column(name="update_time")
    LocalDateTime updatetime;

    // Sửa thành lưu ID để JOIN với bảng User khi cần tra cứu trách nhiệm
    @Column(name="changer_id")
    private String changerId;
}