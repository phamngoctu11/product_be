package com.example.workflow.service;

import com.example.workflow.dto.ReorderItemDTO;
import com.example.workflow.dto.ReorderResponseDTO;
import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.CustomRequestRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.util.JsonUtils;
import com.example.workflow.util.TextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderReorderService {
    private final OrderLookupService orderLookupService;
    private final CustomRequestRepository customRequestRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final CartService cartService;
    private final DurableRequestExecutor durableRequests;
    private final ObjectMapper objectMapper;

    public ReorderResponseDTO reorder(Long orderId, String idempotencyKey) {
        User owner = currentUserService.requireCurrentUser(Role.USER, ConstantErrorCode.REORDER_NOT_ALLOWED);
        userService.requireActiveUser(owner.getId());
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        String result = durableRequests.execute(
                "reorder:user:" + owner.getId() + ":source-order:" + orderId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "reorder request"),
                () -> JsonUtils.write(objectMapper,
                        reorderWithinTransaction(orderId, owner.getId()), "reorder result")
        );
        return JsonUtils.read(objectMapper, result, ReorderResponseDTO.class, "reorder result");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ReorderResponseDTO reorderWithinTransaction(Long orderId, String ownerId) {
        Order source = orderLookupService.requireForUpdate(orderId);
        if (source.getUser() == null || !Objects.equals(source.getUser().getId(), ownerId)
                || (source.getStatus() != OrderStatus.DELIVERED && source.getStatus() != OrderStatus.CANCELLED)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.REORDER_NOT_ALLOWED);
        }
        OrderType type = resolveOrderType(source);
        return type == OrderType.CUSTOM
                ? reorderCustom(source, ownerId)
                : reorderCatalog(source, ownerId);
    }

    private ReorderResponseDTO reorderCatalog(Order source, String ownerId) {
        List<ReorderItemDTO> added = new ArrayList<>();
        List<ReorderItemDTO> skipped = new ArrayList<>();
        for (OrderItem item : source.itemsOrEmpty()) {
            ProductVariant variant = item == null ? null : item.getProductVariant();
            Long variantId = variant == null ? null : variant.getId();
            String variantName = item == null ? null : TextNormalizer.firstPresent(
                    item.getVariantNameSnapshot(), variant == null ? null : variant.getVariantName());
            int quantity = item == null ? 0 : item.getQuantity();
            try {
                validateCatalogItem(item, variant);
                cartService.addToCart(CartService.CartOwner.user(ownerId), variantId, quantity);
                added.add(new ReorderItemDTO(item.getId(), variantId, variantName, quantity, null));
            } catch (AppException exception) {
                skipped.add(new ReorderItemDTO(
                        item == null ? null : item.getId(), variantId, variantName, quantity, exception.getMessage()));
            } catch (RuntimeException exception) {
                skipped.add(new ReorderItemDTO(
                        item == null ? null : item.getId(), variantId, variantName, quantity,
                        StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : "Không thể thêm sản phẩm này vào giỏ hàng."));
            }
        }
        return new ReorderResponseDTO(OrderType.CATALOG, null, added.size(), skipped.size(), added, skipped);
    }

    private ReorderResponseDTO reorderCustom(Order source, String ownerId) {
        OrderItem item = source.itemsOrEmpty().stream()
                .filter(candidate -> candidate != null && candidate.getSourceType() == OrderItemSourceType.CUSTOM)
                .findFirst()
                .orElseGet(() -> source.itemsOrEmpty().stream().findFirst()
                        .orElseThrow(() -> invalid("Custom source order has no item snapshot.")));
        CustomSnapshot snapshot = customSnapshot(source, item);
        CustomRequest draft = new CustomRequest();
        draft.setOwnerId(ownerId);
        draft.setStatus(CustomRequestStatus.DRAFT);
        draft.setDescription(snapshot.description());
        draft.setImageUrls(snapshot.imageUrls());
        draft.setQuantity(Math.max(item.getQuantity(), 1));
        draft.setSourceOrderId(source.getId());
        draft = customRequestRepository.saveAndFlush(draft);
        return new ReorderResponseDTO(OrderType.CUSTOM, draft.getId(), 0, 0, List.of(), List.of());
    }

    private CustomSnapshot customSnapshot(Order source, OrderItem item) {
        String description = null;
        List<String> images = List.of();
        if (StringUtils.hasText(item.getSpecSnapshot())) {
            try {
                JsonNode node = objectMapper.readTree(item.getSpecSnapshot());
                if (node != null && node.isObject()) {
                    description = text(node.get("description"));
                    JsonNode imageNode = node.get("imageUrls");
                    if (imageNode != null && imageNode.isArray()) {
                        List<String> parsed = new ArrayList<>();
                        imageNode.forEach(value -> {
                            if (value.isTextual() && StringUtils.hasText(value.asText())) parsed.add(value.asText().trim());
                        });
                        images = List.copyOf(parsed);
                    }
                } else if (node != null && node.isTextual()) {
                    description = text(node);
                }
            } catch (Exception ignored) {
                description = item.getSpecSnapshot().trim();
            }
        }
        CustomRequest legacy = customRequestRepository.findByLinkedOrderId(source.getId()).orElse(null);
        if (!StringUtils.hasText(description) && legacy != null) description = legacy.getDescription();
        if (images.isEmpty() && legacy != null && legacy.getImageUrls() != null) images = List.copyOf(legacy.getImageUrls());
        if (!StringUtils.hasText(description)) throw invalid("Custom source snapshot has no description.");
        return new CustomSnapshot(description.trim(), images);
    }

    private void validateCatalogItem(OrderItem item, ProductVariant variant) {
        if (item == null || item.getSourceType() == OrderItemSourceType.CUSTOM || item.getQuantity() < 1
                || variant == null || variant.getId() == null || variant.isDelete()
                || variant.getProduct() == null || variant.getProduct().isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND);
        }
        if (variant.getProduct().getAvailabilityStatus() != ProductAvailabilityStatus.ACCEPTING_ORDERS) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.PRODUCT_NOT_ACCEPTING_ORDERS,
                    variant.getProduct().getId());
        }
    }

    private OrderType resolveOrderType(Order order) {
        if (order.getOrderType() != null) return order.getOrderType();
        return order.itemsOrEmpty().stream().anyMatch(item -> item.getSourceType() == OrderItemSourceType.CUSTOM)
                ? OrderType.CUSTOM : OrderType.CATALOG;
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() && StringUtils.hasText(node.asText()) ? node.asText().trim() : null;
    }

    private AppException invalid(String detail) {
        return new AppException(HttpStatus.CONFLICT, ConstantErrorCode.REORDER_INVALID, detail);
    }

    private record CustomSnapshot(String description, List<String> imageUrls) {
    }
}
