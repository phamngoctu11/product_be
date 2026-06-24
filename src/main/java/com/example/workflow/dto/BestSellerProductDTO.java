package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BestSellerProductDTO {
    private Long productId;
    private String productName;
    private Long soldQuantity;
    private String imageUrl;
}
