package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BestSellerProductDTO implements Serializable {
    private Long productId;
    private String productName;
    private Long soldQuantity;
    private String imageUrl;
}
