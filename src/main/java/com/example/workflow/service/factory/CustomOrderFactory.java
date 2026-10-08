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
        order.setTotalPrice(0.0);
        order.setDiscountAmount(0.0);
        order.setFinalPrice(null);
        order.setContactSnapshot(contact);
        order.setItems(new ArrayList<>());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductVariant(null);
        item.setSourceType(OrderItemSourceType.CUSTOM);
        item.setProductNameSnapshot("Sản phẩm custom #" + draft.getId());
        item.setVariantNameSnapshot(null);
        item.setSpecSnapshot(snapshot(draft));
        item.setQuantity(draft.getQuantity());
        item.setPrice(null);
        item.setHandmade(true);
        item.setProductionStatus(OrderItemProductionStatus.WAITING_ASSIGNMENT);
        order.getItems().add(item);
        return order;
    }

    private String snapshot(CustomRequest draft) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("spec", draft.getSpec());
        snapshot.put("attachments", draft.getAttachments());
        return JsonUtils.write(objectMapper, snapshot, "custom request data");
    }
}
