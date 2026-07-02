package com.example.workflow.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResDTO implements Serializable {

    private String user_id;
    private List<CartItemDTO> items;
    private double totalPrice;
}
