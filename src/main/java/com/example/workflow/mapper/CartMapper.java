package com.example.workflow.mapper;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(source="user.id",target="user_id")
    @Mapping(target = "totalPrice", ignore = true)
    CartResDTO toDTO(Cart cart);
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.product_name", target = "productName")
    @Mapping(source = "quantity", target = "quantity")
    @Mapping(source = "price", target = "price")
    CartItemDTO toCartItemDTO(CartItem item);
    @AfterMapping
    default void calculateTotalPrice(Cart cart, @MappingTarget CartResDTO dto) {
        if (dto.getItems() != null) {
            double total = dto.getItems().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
            dto.setTotalPrice(total);
        } else {
            dto.setTotalPrice(0.0);
        }
    }
}
