package com.example.workflow.dto;

import com.example.workflow.nume.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDTO {
    private Long id;
    private OrderStatus oldstatus;
    private OrderStatus newstatus;
    private LocalDateTime updatetime;
    private String changer;
}