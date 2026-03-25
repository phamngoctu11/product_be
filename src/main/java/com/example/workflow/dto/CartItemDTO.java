package com.example.workflow.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO implements Serializable {
    private Long productId;
    private String productName;
    private int quantity;
    private double price;
    @JsonProperty("image_url")
    private String imageUrl;
}