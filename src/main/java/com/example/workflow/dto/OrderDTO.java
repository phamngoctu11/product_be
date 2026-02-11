package com.example.workflow.dto;
import com.example.workflow.nume.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderDTO {
    private Long user_id;
    private List<OrderItemDTO> items;
    private double totalPrice;
    LocalDateTime startOrderTime;
    LocalDateTime endOrderTime;
    OrderStatus status;
    String cancelReason;
}
