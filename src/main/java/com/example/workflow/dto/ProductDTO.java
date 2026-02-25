package com.example.workflow.dto;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProductDTO implements Serializable {
    Long id;
    String product_name;
    double price;
    int quantity;
}
