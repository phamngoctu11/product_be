package com.example.workflow.dto;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrderItemDTO implements Serializable {
    private Long productId;
    private String productName;
    private int quantity;
    private double price;
}
