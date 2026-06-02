package com.example.workflow.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrderItemDTO implements Serializable {
    private Long variantId;
    private String variantName;
    private int quantity;
    private Integer exportedQuantity;
    private Integer receivedQuantity;
    private double price;
    @JsonProperty("image_url")
    private String imageUrl;
}
