package com.example.workflow.dto;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrderItemDTO implements Serializable {
    private Long orderItemId;
    private Long variantId;
    private Long productId;
    private String productName;
    private String variantName;
    private String attributes;
    private int quantity;
    private Integer exportedQuantity;
    private Integer receivedQuantity;
    private double price;
    private boolean handmade;
    private OrderItemProductionStatus productionStatus;
    private boolean reviewed;
    private Long reviewId;
    @JsonProperty("image_url")
    private String imageUrl;
}
