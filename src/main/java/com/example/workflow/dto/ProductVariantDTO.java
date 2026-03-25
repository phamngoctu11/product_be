package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProductVariantDTO implements Serializable {
    private Long id;
    private String variantName; // VD: Áo khoác da cá sấu
    private double price;       // Giá riêng của loại da này
    private int quantity;       // Số lượng kho riêng

    // Cột attributes sẽ chứa chuỗi JSON (VD: "{\"material\": \"Da cá sấu\", \"size\": \"XL\"}")
    private String attributes;
    @JsonProperty("image_url")
    private String imageUrl;
}