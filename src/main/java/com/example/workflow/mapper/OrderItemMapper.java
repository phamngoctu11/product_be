package com.example.workflow.mapper;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderItemDTO;
import com.example.workflow.dto.ProductTypeFieldDTO;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.nume.OrderItemSourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    @Mapping(target = "productTypeCode", ignore = true)
    @Mapping(target = "productTypeName", ignore = true)
    @Mapping(target = "productTypeFields", ignore = true)
    @Mapping(target = "fieldValues", ignore = true)
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
        target.setProductTypeCode(content.productTypeCode());
        target.setProductTypeName(content.productTypeName());
        target.setProductTypeFields(content.productTypeFields());
        target.setFieldValues(content.fieldValues());
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
        request.setOrderItemId(orderItem.getId());
        request.setReceivedQuantity(orderItem.getExportedQuantity() != null
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
            return CustomContent.legacy(null);
        }
        try {
            JsonNode root = SnapshotJson.OBJECT_MAPPER.readTree(snapshot);
            if (!root.isObject()) {
                return CustomContent.legacy(snapshot);
            }
            JsonNode description = root.has("description") ? root.get("description") : root.get("spec");
            JsonNode images = root.has("imageUrls") ? root.get("imageUrls") : root.get("attachments");
            JsonNode type = root.get("productType");
            JsonNode fields = root.get("productTypeFields");
            JsonNode values = root.get("fieldValues");
            return new CustomContent(text(description), imageUrls(images),
                    type == null ? null : text(type.get("code")), type == null ? null : text(type.get("name")),
                    fields == null || !fields.isArray() ? List.of() : SnapshotJson.OBJECT_MAPPER.convertValue(fields, new TypeReference<>() {}),
                    values == null || !values.isObject() ? Map.of() : SnapshotJson.OBJECT_MAPPER.convertValue(values, new TypeReference<>() {}));
        } catch (JsonProcessingException ignored) {
            // Some legacy custom rows stored the description directly instead of a JSON snapshot.
            return CustomContent.legacy(snapshot);
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

    record CustomContent(String description, List<String> imageUrls, String productTypeCode, String productTypeName,
                         List<ProductTypeFieldDTO> productTypeFields, Map<String, Object> fieldValues) {
        static CustomContent legacy(String description) { return new CustomContent(description, List.of(), null, null, List.of(), Map.of()); }
    }

    final class SnapshotJson {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        private SnapshotJson() {
        }
    }
}
