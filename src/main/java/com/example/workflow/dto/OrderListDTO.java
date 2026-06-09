package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListDTO implements Serializable {
    private Long id;
    private String customerName;
    private double finalPrice;
    private OrderStatus status;
    private LocalDateTime startOrderTime;
    private String paymentMethod;
    private String staffName;
    public OrderListDTO(Long id, String customerName, double finalPrice, OrderStatus status, LocalDateTime startOrderTime, String paymentMethod) {
        this.id = id;
        this.customerName = customerName;
        this.finalPrice = finalPrice;
        this.status = status;
        this.startOrderTime = startOrderTime;
        this.paymentMethod = paymentMethod;
    }

}
