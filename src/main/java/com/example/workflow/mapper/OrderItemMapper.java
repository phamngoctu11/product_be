package com.example.workflow.mapper;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.nume.OrderItemSourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
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
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "imageUrls", ignore = true)
    // Price is read from the OrderItem snapshot, never recalculated from the current variant.
    OrderItemDTO toDto(OrderItem orderItem);

    @AfterMapping
    default void mapCustomContent(OrderItem orderItem, @MappingTarget OrderItemDTO target) {
        if (orderItem == null || orderItem.getSourceType() != OrderItemSourceType.CUSTOM) {
            target.setImageUrls(List.of());
            return;
        }

        CustomContent content = parseCustomContent(orderItem.getSpecSnapshot());
        target.setDescription(content.description());
        target.setImageUrls(content.imageUrls());
    }

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

    private CustomContent parseCustomContent(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return new CustomContent(null, List.of());
        }
        try {
            JsonNode root = SnapshotJson.OBJECT_MAPPER.readTree(snapshot);
            if (!root.isObject()) {
                return new CustomContent(snapshot, List.of());
            }
            JsonNode description = root.has("description") ? root.get("description") : root.get("spec");
            JsonNode images = root.has("imageUrls") ? root.get("imageUrls") : root.get("attachments");
            return new CustomContent(text(description), imageUrls(images));
        } catch (JsonProcessingException ignored) {
            // Some legacy custom rows stored the description directly instead of a JSON snapshot.
            return new CustomContent(snapshot, List.of());
        }
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private List<String> imageUrls(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(value -> {
                if (value.isTextual()) {
                    values.add(value.asText());
                }
            });
            return List.copyOf(values);
        }
        if (node.isTextual()) {
            String raw = node.asText();
            if (raw.isBlank()) {
                return List.of();
            }
            try {
                JsonNode nested = SnapshotJson.OBJECT_MAPPER.readTree(raw);
                if (nested.isArray()) {
                    return imageUrls(nested);
                }
            } catch (JsonProcessingException ignored) {
                // Legacy attachments may contain one raw URL rather than a JSON array.
            }
            return List.of(raw);
        }
        return List.of();
    }

    record CustomContent(String description, List<String> imageUrls) {
    }

    final class SnapshotJson {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        private SnapshotJson() {
        }
    }
}
