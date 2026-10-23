package com.example.workflow.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO implements Serializable {

    private Long cartItemId;
    private Long variantId;
    private String variantName;
    private int quantity;
    private BigDecimal price;
    @JsonProperty("image_url")
    private String imageUrl;

    public CartItemDTO(Long variantId, String variantName, int quantity, BigDecimal price, String imageUrl) {
        this.variantId = variantId;
        this.variantName = variantName;
        this.quantity = quantity;
        this.price = price;
        this.imageUrl = imageUrl;
    }
}
