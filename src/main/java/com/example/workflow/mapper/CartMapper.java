package com.example.workflow.mapper;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface CartMapper {
    @Mapping(source = "user.id", target = "user_id")
    @Mapping(target = "totalPrice", ignore = true)
    CartResDTO toDto(Cart cart);

    @Mapping(source = "id", target = "cartItemId")
    @Mapping(source = "productVariant.id", target = "variantId")
    @Mapping(source = "productVariant.variantName", target = "variantName")
    @Mapping(source = "productVariant.price", target = "price")
    @Mapping(target = "imageUrl", expression = "java(resolveImageUrl(cartItem))")
    CartItemDTO toDto(CartItem cartItem);

    List<CartItemDTO> toItemDtos(List<CartItem> cartItems);

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

    default String resolveImageUrl(CartItem item) {
        if (item == null || item.getProductVariant() == null) {
            return null;
        }
        var variant = item.getProductVariant();
        if (variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) {
            return variant.getImageUrl();
        }
        return variant.getProduct() == null ? null : variant.getProduct().getImageUrl();
    }
}
