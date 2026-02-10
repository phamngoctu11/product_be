package com.example.workflow.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResDTO {
    private Long user_id;
    private List<CartItemDTO> items;
    private double totalPrice;
}