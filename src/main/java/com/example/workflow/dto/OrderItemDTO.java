package com.example.workflow.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrderItemDTO implements Serializable {
    private Long productId;
    private String productName;
    private int quantity;
    private double price;
    @JsonProperty("image_url")
    private String imageUrl;
}
