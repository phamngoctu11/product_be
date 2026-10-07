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
    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "productVariant.id", target = "variantId")
    @Mapping(source = "productVariant.product.id", target = "productId")
    @Mapping(target = "productName", expression = "java(resolveProductName(orderItem))")
    @Mapping(target = "variantName", expression = "java(resolveVariantName(orderItem))")
    @Mapping(source = "productVariant.attributes", target = "attributes")
    @Mapping(target = "reviewed", ignore = true)
    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    // Price is read from the OrderItem snapshot, never recalculated from the current variant.
    OrderItemDTO toDto(OrderItem orderItem);

    default String resolveProductName(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        if (orderItem.getProductNameSnapshot() != null && !orderItem.getProductNameSnapshot().isBlank()) {
            return orderItem.getProductNameSnapshot();
        }
        if (orderItem.getProductVariant() == null || orderItem.getProductVariant().getProduct() == null) {
            return null;
        }
        return orderItem.getProductVariant().getProduct().getProductName();
    }

    default String resolveVariantName(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        if (orderItem.getVariantNameSnapshot() != null && !orderItem.getVariantNameSnapshot().isBlank()) {
            return orderItem.getVariantNameSnapshot();
        }
        return orderItem.getProductVariant() == null ? null : orderItem.getProductVariant().getVariantName();
    }

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
