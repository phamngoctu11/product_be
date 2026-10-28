package com.example.workflow.service.factory;

import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderContactSnapshot;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOrderFactory {
    private final ObjectMapper objectMapper;

    public Order create(User owner, CustomRequest draft, OrderContactSnapshot contact) {
        Order order = new Order();
        order.setUser(owner);
        order.setOrderType(OrderType.CUSTOM);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setPaymentStatus(PaymentStatus.NOT_DUE);
        order.setPaymentMethod(null);
        order.setPaymentMethodType(null);
        order.setProductionStatus(OrderProductionStatus.WAITING_PRODUCTION);
        order.setStartOrderTime(LocalDateTime.now());
        order.setTotalPrice(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalPrice(null);
        order.setContactSnapshot(contact);
        order.setItems(new ArrayList<>());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductVariant(null);
        item.setSourceType(OrderItemSourceType.CUSTOM);
        item.setProductNameSnapshot("Sản phẩm tùy chỉnh #" + draft.getId());
        item.setVariantNameSnapshot(null);
        item.setSpecSnapshot(snapshot(draft));
        item.setQuantity(draft.getQuantity());
        item.setPrice(null);
        item.setProductionStatus(OrderItemProductionStatus.WAITING_ASSIGNMENT);
        order.getItems().add(item);
        return order;
    }

    private String snapshot(CustomRequest draft) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("description", draft.getDescription());
        snapshot.put("imageUrls", draft.getImageUrls());
        snapshot.put("productType", draft.getProductType() == null ? null : Map.of(
                "id", draft.getProductType().getId(),
                "code", draft.getProductType().getCode(),
                "name", draft.getProductType().getName()
        ));
        snapshot.put("productTypeFields", readJson(draft.getProductTypeSchemaSnapshot(), java.util.List.of()));
        snapshot.put("fieldValues", readJson(draft.getFieldValuesJson(), Map.of()));
        return JsonUtils.write(objectMapper, snapshot, "custom request data");
    }

    private Object readJson(String json, Object fallback) {
        if (json == null || json.isBlank()) return fallback;
        try { return objectMapper.readValue(json, Object.class); }
        catch (Exception exception) { return fallback; }
    }
}
