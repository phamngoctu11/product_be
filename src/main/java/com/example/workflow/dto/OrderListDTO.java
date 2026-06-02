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
    private double totalPrice;
    private OrderStatus status;
    private LocalDateTime startOrderTime;
    private String paymentMethod;
}
