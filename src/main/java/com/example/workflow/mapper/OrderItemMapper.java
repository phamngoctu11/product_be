package com.example.workflow.mapper;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(source = "productVariant.id", target = "variantId")
    @Mapping(source = "productVariant.variantName", target = "variantName")
        // Note: Trường price của orderItem không cần map từ variant vì bảng OrderItem đã lưu sẵn price lúc chốt đơn rồi
    OrderItemDTO toDto(OrderItem orderItem);
    default ItemCheckRequest toCheckRequest(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        ItemCheckRequest request = new ItemCheckRequest();
        if (orderItem.getProductVariant() != null) {
            request.setVariantId(orderItem.getProductVariant().getId());
        }
        request.setQuantity(orderItem.getExportedQuantity() != null
                ? orderItem.getExportedQuantity()
                : orderItem.getQuantity());
        return request;
    }

    default List<ItemCheckRequest> toCheckRequest(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return Collections.emptyList();
        }
        return orderItems.stream()
                .map(this::toCheckRequest)
                .collect(Collectors.toList());
    }
}
