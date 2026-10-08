package com.example.workflow.mapper;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(config = CentralMapperConfig.class)
public interface OrderItemMapper {
    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "productVariant.id", target = "variantId")
    @Mapping(source = "productVariant.product.id", target = "productId")
    @Mapping(target = "productName", expression = "java(resolveProductName(orderItem))")
    @Mapping(target = "variantName", expression = "java(resolveVariantName(orderItem))")
    @Mapping(target = "attributes", expression = "java(resolveAttributes(orderItem))")
    @Mapping(target = "reviewed", ignore = true)
    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "imageUrl", expression = "java(resolveImageUrl(orderItem))")
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

    default String resolveAttributes(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        if (orderItem.getSpecSnapshot() != null && !orderItem.getSpecSnapshot().isBlank()) {
            return orderItem.getSpecSnapshot();
        }
        return orderItem.getProductVariant() == null ? null : orderItem.getProductVariant().getAttributes();
    }

    default String resolveImageUrl(OrderItem orderItem) {
        if (orderItem == null || orderItem.getProductVariant() == null) {
            return null;
        }
        var variant = orderItem.getProductVariant();
        if (variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) {
            return variant.getImageUrl();
        }
        return variant.getProduct() == null ? null : variant.getProduct().getImageUrl();
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
