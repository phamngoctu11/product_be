package com.example.workflow.mapper;

import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(source = "productVariant.id", target = "productId")
    @Mapping(source = "productVariant.variantName", target = "productName")
        // Note: Trường price của orderItem không cần map từ variant vì bảng OrderItem đã lưu sẵn price lúc chốt đơn rồi
    OrderItemDTO toDto(OrderItem orderItem);
}
