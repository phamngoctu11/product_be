package com.example.workflow.mapper;

import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.product_name", target = "productName")
    OrderItemDTO toDto(OrderItem orderItem);
}
