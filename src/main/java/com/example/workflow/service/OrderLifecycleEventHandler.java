package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.event.payload.OrderDeliveredEvent;
import com.example.workflow.event.payload.PaymentConfirmedEvent;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OrderLifecycleEventHandler {
    private final OrderRepository orderRepository;
    private final ConsultationAttributionService consultationAttributionService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ApplicationCacheService applicationCacheService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_CREATED");
        consultationAttributionService.recordOrderAttributions(order);
        applicationCacheService.evictOrderCreated(order);

        if (order.getUser() == null) {
            return;
        }
        if (StringUtils.hasText(order.getEmail())) {
            if (order.getOrderType() == OrderType.CUSTOM) {
                OrderItem item = firstItem(order);
                emailService.sendCustomOrderConfirmationEmail(
                        order.getEmail(),
                        order.getRecipientName(),
                        order.getId(),
                        customSpec(item),
                        item == null ? null : item.getQuantity()
                );
            } else {
                emailService.sendOrderConfirmationEmail(
                        order.getEmail(),
                        order.getRecipientName(),
                        order.getId(),
                        order.getFinalPrice(),
                        paymentLabel(order)
                );
            }
        }

        String userId = order.getUser().getId();
        String title = order.getOrderType() == OrderType.CUSTOM
                ? "Yêu cầu custom đã được tạo"
                : "Đặt hàng thành công";
        String content = order.getOrderType() == OrderType.CUSTOM
                ? "Yêu cầu custom cho đơn #" + order.getId() + " đã được tạo và đang chờ quản lý duyệt."
                : "Đơn hàng #" + order.getId() + " đã được tạo và đang chờ quản lý duyệt.";
        notificationService.sendNotification(
                title,
                content,
                order.getId(),
                userId,
                null,
                "/topic/user-notifications/" + userId
        );
    }

    public void handleGuestOrderCreated(GuestOrderCreatedEvent event) {
        Order order = requireOrder(event.orderId(), "GUEST_ORDER_CREATED");
        if (order.getUser() != null || !StringUtils.hasText(order.getEmail())) {
            return;
        }

        applicationCacheService.evictOrderCreated(order);
        String accessUrl = StringUtils.hasText(event.lookupToken())
                ? UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                        .path("/guest/orders/")
                        .path(event.orderId().toString())
                        .queryParam("token", event.lookupToken())
                        .build()
                        .encode()
                        .toUriString()
                : null;
        emailService.sendGuestOrderConfirmationEmail(
                order.getEmail(),
                order.getRecipientName(),
                order.getId(),
                order.getFinalPrice(),
                "Thanh toán khi nhận hàng (COD)",
                accessUrl,
                event.productionDurationDays()
        );
    }

    public void handleOrderDelivered(OrderDeliveredEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_DELIVERED");
        consultationAttributionService.confirmOrderAttributions(order.getId());
        if (order.getUser() == null) {
            return;
        }

        String userId = order.getUser().getId();
        notificationService.sendNotification(
                "Đánh giá sản phẩm",
                "Đơn hàng #" + order.getId() + " đã hoàn tất. Hãy chia sẻ trải nghiệm của bạn cho từng sản phẩm.",
                order.getId(),
                userId,
                null,
                "/topic/user-notifications/" + userId
        );
        applicationCacheService.evictOrderDelivered(order, userId);
    }

    public void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_CANCELLED");
        consultationAttributionService.cancelOrderAttributions(order.getId());
        applicationCacheService.evictOrderCancelled(order, event.oldStatus());

        if (StringUtils.hasText(order.getEmail())) {
            emailService.sendOrderCancellationEmail(
                    order.getEmail(),
                    order.getRecipientName(),
                    order.getId(),
                    event.reason()
            );
        }
        if (order.getUser() != null) {
            String userId = order.getUser().getId();
            notificationService.sendNotification(
                    "Đơn hàng đã hủy",
                    cancellationMessage(order.getId(), event.reason()),
                    order.getId(),
                    userId,
                    null,
                    "/topic/user-notifications/" + userId
            );
        }
        String assignedStaffId = StringUtils.hasText(event.assignedStaffId())
                ? event.assignedStaffId()
                : assignedStaffId(order);
        if (StringUtils.hasText(assignedStaffId)) {
            notificationService.sendNotification(
                    "Đơn phụ trách đã hủy",
                    cancellationMessage(order.getId(), event.reason()),
                    order.getId(),
                    assignedStaffId,
                    null,
                    "/topic/user-notifications/" + assignedStaffId
            );
        }
        notificationService.sendNotification(
                "Đơn hàng đã hủy",
                cancellationMessage(order.getId(), event.reason()),
                order.getId(),
                null,
                null,
                "/topic/admin-notifications"
        );
    }

    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        Order order = requireOrder(event.orderId(), "PAYMENT_CONFIRMED");
        if (StringUtils.hasText(order.getEmail())) {
            emailService.sendOrderConfirmationEmail(
                    order.getEmail(),
                    order.getRecipientName(),
                    order.getId(),
                    order.getFinalPrice(),
                    "Thanh toán online đã xác nhận"
            );
        }
        if (order.getUser() != null) {
            String userId = order.getUser().getId();
            notificationService.sendNotification(
                    "Thanh toán thành công",
                    "Thanh toán cho đơn hàng #" + order.getId() + " đã được xác nhận.",
                    order.getId(),
                    userId,
                    null,
                    "/topic/user-notifications/" + userId
            );
        }

        String staffId = assignedStaffId(order);
        if (StringUtils.hasText(staffId)) {
            notificationService.sendNotification(
                    "Đơn hàng đã thanh toán",
                    "Đơn hàng #" + order.getId() + " đã thanh toán và đang chờ bạn bắt đầu sản xuất.",
                    order.getId(),
                    staffId,
                    null,
                    "/topic/user-notifications/" + staffId
            );
        }
    }

    private Order requireOrder(Long orderId, String eventType) {
        if (orderId == null) {
            throw new IllegalArgumentException(eventType + " event must contain orderId");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found for " + eventType + " event: " + orderId));
    }

    private OrderItem firstItem(Order order) {
        return order.getItems() == null || order.getItems().isEmpty() ? null : order.getItems().getFirst();
    }

    private String customSpec(OrderItem item) {
        if (item == null || !StringUtils.hasText(item.getSpecSnapshot())) {
            return null;
        }
        try {
            JsonNode snapshot = objectMapper.readTree(item.getSpecSnapshot());
            JsonNode spec = snapshot.get("spec");
            return spec == null || spec.isNull() ? null : spec.asText();
        } catch (JsonProcessingException ignored) {
            return item.getSpecSnapshot();
        }
    }

    private String paymentLabel(Order order) {
        PaymentMethod method = order.getPaymentMethodType();
        if (method == null && StringUtils.hasText(order.getPaymentMethod())) {
            try {
                method = PaymentMethod.valueOf(order.getPaymentMethod());
            } catch (IllegalArgumentException ignored) {
                return order.getPaymentMethod();
            }
        }
        return method == PaymentMethod.ONLINE
                ? "Thanh toán online sau khi đơn được duyệt"
                : "Thanh toán khi nhận hàng (COD)";
    }

    private String assignedStaffId(Order order) {
        if (order.getAssignedStaff() != null) {
            return order.getAssignedStaff().getId();
        }
        return order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
    }

    private String cancellationMessage(Long orderId, String reason) {
        String message = "Đơn hàng #" + orderId + " đã bị hủy.";
        return StringUtils.hasText(reason) ? message + " Lý do: " + reason : message;
    }
}
