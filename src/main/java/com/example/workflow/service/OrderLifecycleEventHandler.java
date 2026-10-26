package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAgreement;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.event.payload.GuestOrderCreatedEvent;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.event.payload.OrderAcceptedEvent;
import com.example.workflow.event.payload.OrderAssignedEvent;
import com.example.workflow.event.payload.OrderAgreementSubmittedEvent;
import com.example.workflow.event.payload.OrderAgreementReviewedEvent;
import com.example.workflow.event.payload.OrderDeliveredEvent;
import com.example.workflow.event.payload.OrderShippingEvent;
import com.example.workflow.event.payload.PaymentConfirmedEvent;
import com.example.workflow.event.payload.PaymentOpenedEvent;
import com.example.workflow.event.payload.ProductionStartedEvent;
import com.example.workflow.event.payload.ProductionCheckpointSubmittedEvent;
import com.example.workflow.event.payload.ProductionCheckpointReviewedEvent;
import com.example.workflow.event.payload.OrderReadyToShipEvent;
import com.example.workflow.event.payload.OrderReviewedEvent;
import com.example.workflow.event.payload.ChangeRequestSubmittedEvent;
import com.example.workflow.event.payload.ChangeRequestDecidedEvent;
import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionDecisionType;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderAgreementRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderLifecycleEventHandler {
    private final OrderRepository orderRepository;
    private final OrderAgreementRepository orderAgreementRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ApplicationCacheService applicationCacheService;
    private final OrderWorkflowService orderWorkflowService;
    private final OrderChatService orderChatService;
    private final GuestPurchaseWorkflowStateService guestWorkflowStateService;
    private final OrderLookupTokenService orderLookupTokenService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public void handleChangeRequestSubmitted(ChangeRequestSubmittedEvent event) {
        requireOrder(event.orderId(), "CHANGE_REQUEST_SUBMITTED");
    }

    public void handleChangeRequestDecided(ChangeRequestDecidedEvent event) {
        requireOrder(event.orderId(), "CHANGE_REQUEST_DECIDED");
    }

    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_CREATED");
        applicationCacheService.evictOrderCreated(order);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            startLifecycle(order);
        }

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
                        item == null ? null : item.getQuantity(),
                        authenticatedOrderUrl(order.getId())
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
                "/topic/user-notifications/" + userId
        );
    }

    public void handleOrderAccepted(OrderAcceptedEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_ACCEPTED");
        if (StringUtils.hasText(order.getEmail())) {
            emailService.sendOrderAcceptedEmail(
                    order.getEmail(),
                    order.getRecipientName(),
                    order.getId(),
                    order.getUser() == null ? signedGuestOrderUrl(order) : authenticatedOrderUrl(order.getId()),
                    catalogSummary(order),
                    order.getFinalPrice(),
                    totalQuantity(order),
                    maximumDurationDays(order)
            );
        }
        if (order.getUser() != null) {
            String userId = order.getUser().getId();
            notificationService.sendNotification(
                    "Đơn hàng đã được chấp nhận",
                    "Đơn hàng #" + order.getId() + " đã được chấp nhận và đang chờ bước tiếp theo.",
                    order.getId(),
                    userId,
                    "/topic/user-notifications/" + userId
            );
        }
    }

    public void handleGuestOrderCreated(GuestOrderCreatedEvent event) {
        Order order = requireOrder(event.orderId(), "GUEST_ORDER_CREATED");
        if (order.getUser() != null || !StringUtils.hasText(order.getEmail())) {
            return;
        }

        applicationCacheService.evictOrderCreated(order);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            OrderWorkflowService.StartResult workflow = startLifecycle(order);
            guestWorkflowStateService.markStarted(order.getId(), workflow.processInstanceId());
        }
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

    public void handleOrderAssigned(OrderAssignedEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_ASSIGNED");
        if (order.getStatus() == OrderStatus.CANCELLED
                || !Objects.equals(event.staffId(), assignedStaffId(order))) {
            return;
        }
        startLifecycle(order);
        if (order.getUser() != null) {
            orderChatService.ensureThreadForAssignedOrder(order.getId());
        }
        requireWorkflowAdvance(
                order.getId(),
                orderWorkflowService.advanceAfterAssignment(order.getId(), event.staffId()),
                "ORDER_ASSIGNED"
        );
    }

    public void handleOrderReviewed(OrderReviewedEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_REVIEWED");
        boolean approved = event.decision() == com.example.workflow.nume.ManagerReviewDecision.APPROVED;
        if (approved) {
            startLifecycle(order);
        }
        requireWorkflowAdvance(
                order.getId(),
                orderWorkflowService.completeManagerReview(
                        order.getId(), approved, event.managerId(), event.confirmationDueAt()
                ),
                "ORDER_REVIEWED"
        );
        if (approved && StringUtils.hasText(event.assignedStaffId())) {
            requireWorkflowAdvance(
                    order.getId(),
                    orderWorkflowService.advanceAfterAssignment(order.getId(), event.assignedStaffId()),
                    "ORDER_REVIEWED_ASSIGNMENT"
            );
        }
    }

    public void handleOrderAgreementSubmitted(OrderAgreementSubmittedEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_AGREEMENT_SUBMITTED");
        OrderAgreement agreement = orderAgreementRepository
                .findByOrderIdAndAgreementVersion(event.orderId(), event.agreementVersion())
                .orElse(null);
        if (order.getCurrentAgreementVersion() == null
                || order.getCurrentAgreementVersion() < event.agreementVersion()
                || agreement == null || agreement.getSubmittedAt() == null) {
            return;
        }
        requireWorkflowAdvance(
                order.getId(),
                orderWorkflowService.correlateAgreementSubmitted(
                        event.orderId(),
                        event.agreementVersion(),
                        event.paymentMethod() == null ? null : event.paymentMethod().name()
                ),
                "ORDER_AGREEMENT_SUBMITTED"
        );
    }

    public void handleOrderAgreementReviewed(OrderAgreementReviewedEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_AGREEMENT_REVIEWED");
        if (order.getCurrentAgreementVersion() == null
                || !order.getCurrentAgreementVersion().equals(event.agreementVersion())) {
            return;
        }
        requireWorkflowAdvance(
                order.getId(),
                orderWorkflowService.completeAgreementReview(
                        event.orderId(), event.agreementVersion(), event.decision().name(), event.staffId()
                ),
                "ORDER_AGREEMENT_REVIEWED"
        );
    }

    public void handleOrderDelivered(OrderDeliveredEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_DELIVERED");
        requireWorkflowAdvance(order.getId(), orderWorkflowService.correlateDelivered(order.getId()), "ORDER_DELIVERED");
        if (order.getUser() == null) {
            return;
        }

        String userId = order.getUser().getId();
        if (order.getOrderType() == OrderType.CATALOG) {
            notificationService.sendNotification(
                    "Đánh giá sản phẩm",
                    "Đơn hàng #" + order.getId() + " đã hoàn tất. Hãy chia sẻ trải nghiệm của bạn cho từng sản phẩm.",
                    order.getId(),
                    userId,
                    "/topic/user-notifications/" + userId
            );
        }
        applicationCacheService.evictOrderDelivered(order, userId);
    }

    public void handleOrderShipping(OrderShippingEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_SHIPPING");
        requireWorkflowAdvance(
                order.getId(),
                orderWorkflowService.correlateShipping(order.getId(), event.staffId()),
                "ORDER_SHIPPING"
        );
        applicationCacheService.evictShippingHandover(order, event.staffId());
    }

    public void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_CANCELLED");
        orderWorkflowService.correlateOrderCancelled(order.getId());
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
                    "/topic/user-notifications/" + assignedStaffId
            );
        }
        notificationService.sendNotification(
                "Đơn hàng đã hủy",
                cancellationMessage(order.getId(), event.reason()),
                order.getId(),
                null,
                "/topic/admin-notifications"
        );
    }

    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        Order order = requireOrder(event.orderId(), "PAYMENT_CONFIRMED");
        if (order.getPaymentStatus() != PaymentStatus.PAID) return;
        orderWorkflowService.correlatePaymentSuccess(order.getId());

        String staffId = assignedStaffId(order);
        if (StringUtils.hasText(staffId)) {
            notificationService.sendNotification(
                    "Đơn hàng đã thanh toán",
                    "Đơn hàng #" + order.getId() + " đã thanh toán và đang chờ bạn bắt đầu sản xuất.",
                    order.getId(),
                    staffId,
                    "/topic/user-notifications/" + staffId
            );
        }
    }

    public void handlePaymentOpened(PaymentOpenedEvent event) {
        Order order = requireOrder(event.orderId(), "PAYMENT_OPENED");
        if (order.getStatus() != OrderStatus.ORDER_ACCEPTED
                || order.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }
        orderWorkflowService.correlatePaymentOpened(event.orderId(), event.attemptId(), event.dueAt());
    }

    public void handleProductionStarted(ProductionStartedEvent event) {
        Order order = requireOrder(event.orderId(), "PRODUCTION_STARTED");
        if (order.getStatus() != OrderStatus.ORDER_CREATING
                || order.getProductionStartedAt() == null
                || !Objects.equals(event.staffId(), assignedStaffId(order))) {
            return;
        }
        orderWorkflowService.completeProductionStart(
                event.orderId(), event.staffId(), event.productionStartedAt()
        );
        if (StringUtils.hasText(order.getEmail())) {
            emailService.sendProductionStartedEmail(
                    order.getEmail(),
                    order.getRecipientName(),
                    order.getId(),
                    order.getUser() == null ? signedGuestOrderUrl(order) : authenticatedOrderUrl(order.getId())
            );
        }
        if (order.getUser() != null) {
            String userId = order.getUser().getId();
            notificationService.sendNotification(
                    "Đơn hàng đã bắt đầu sản xuất",
                    "Nhân viên đã bắt đầu sản xuất đơn hàng #" + order.getId() + ".",
                    order.getId(),
                    userId,
                    "/topic/user-notifications/" + userId
            );
        }
    }

    public void handleProductionCheckpointSubmitted(ProductionCheckpointSubmittedEvent event) {
        Order order = requireOrder(event.orderId(), "PRODUCTION_CHECKPOINT_SUBMITTED");
        if (order.getStatus() == OrderStatus.CANCELLED
                || !Objects.equals(event.staffId(), assignedStaffId(order))) {
            return;
        }
        orderWorkflowService.completeProductionCheckpointSubmission(
                event.orderId(), event.orderItemId(), event.checkpointId(), event.stage(), event.attemptNumber()
        );
        if (event.stage() == ProductionCheckpointStage.INITIAL_SHAPE) {
            if (StringUtils.hasText(order.getEmail())) {
                emailService.sendInitialShapeCheckpointEmail(
                        order.getEmail(),
                        order.getRecipientName(),
                        order.getId(),
                        event.orderItemId(),
                        event.imageUrls(),
                        event.note(),
                        order.getUser() == null ? signedGuestOrderUrl(order) : authenticatedOrderUrl(order.getId())
                );
            }
            if (order.getUser() != null) {
                String userId = order.getUser().getId();
                notificationService.sendNotification(
                        "Cập nhật tiến độ sản xuất",
                        "Đã có hình ảnh đầu tiên của item #" + event.orderItemId(),
                        order.getId(),
                        userId,
                        "/topic/user-notifications/" + userId
                );
            }
            return;
        }
        notificationService.sendNotification(
                "Sản phẩm chờ KCS",
                "Item #" + event.orderItemId() + " của đơn #" + order.getId()
                        + " có sản phẩm hoàn thiện lần " + event.attemptNumber() + " đang chờ KCS.",
                order.getId(),
                null,
                "/topic/admin-notifications"
        );
    }

    public void handleProductionCheckpointReviewed(ProductionCheckpointReviewedEvent event) {
        Order order = requireOrder(event.orderId(), "PRODUCTION_CHECKPOINT_REVIEWED");
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        orderWorkflowService.completeProductionCheckpointReview(
                event.orderId(), event.orderItemId(), event.checkpointId(), event.decision()
        );
        if (event.decision() == ProductionDecisionType.REWORK_REQUIRED
                && StringUtils.hasText(event.assignedStaffId())) {
            notificationService.sendNotification(
                    "KCS yêu cầu làm lại",
                    "Item #" + event.orderItemId() + " của đơn #" + order.getId()
                            + " cần làm lại. Lý do: " + event.reason(),
                    order.getId(),
                    event.assignedStaffId(),
                    "/topic/user-notifications/" + event.assignedStaffId()
            );
        }
    }

    public void handleOrderReadyToShip(OrderReadyToShipEvent event) {
        Order order = requireOrder(event.orderId(), "ORDER_READY_TO_SHIP");
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        orderWorkflowService.completeProduction(event.orderId());
        if (StringUtils.hasText(order.getEmail())) {
            emailService.sendReadyToShipEmail(
                    order.getEmail(),
                    order.getRecipientName(),
                    order.getId(),
                    order.getUser() == null ? signedGuestOrderUrl(order) : authenticatedOrderUrl(order.getId())
            );
        }
        if (order.getUser() != null) {
            String userId = order.getUser().getId();
            notificationService.sendNotification(
                    "Đơn hàng sẵn sàng bàn giao",
                    "Đơn hàng #" + order.getId() + " đã hoàn tất sản xuất.",
                    order.getId(),
                    userId,
                    "/topic/user-notifications/" + userId
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

    private OrderWorkflowService.StartResult startLifecycle(Order order) {
        String paymentMethod = resolvePaymentMethod(order);
        return orderWorkflowService.startAfterOrderCreated(
                order.getId(),
                order.getUser() == null,
                order.getOrderType() == null ? OrderType.CATALOG.name() : order.getOrderType().name(),
                paymentMethod,
                order.getItems() == null
                        ? java.util.List.of()
                        : order.getItems().stream().map(OrderItem::getId).filter(Objects::nonNull).toList()
        );
    }

    private String resolvePaymentMethod(Order order) {
        if (order.getPaymentMethodType() != null) return order.getPaymentMethodType().name();
        if (StringUtils.hasText(order.getPaymentMethod())) return order.getPaymentMethod().trim().toUpperCase();
        return "PENDING";
    }

    private void requireWorkflowAdvance(Long orderId, boolean advanced, String eventType) {
        if (!advanced && orderWorkflowService.hasActiveProcess(orderId)) {
            throw new IllegalStateException(
                    "Workflow for order " + orderId + " is not ready for " + eventType
            );
        }
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
            JsonNode description = snapshot.get("description");
            if (description == null || description.isNull()) {
                description = snapshot.get("spec");
            }
            return description == null || description.isNull() ? null : description.asText();
        } catch (JsonProcessingException ignored) {
            return item.getSpecSnapshot();
        }
    }

    private String authenticatedOrderUrl(Long orderId) {
        return UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                .path("/orders/")
                .path(orderId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String signedGuestOrderUrl(Order order) {
        String token = orderLookupTokenService.issueSignedReadToken(order, Duration.ofDays(30));
        return UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                .path("/guest/orders/")
                .path(order.getId().toString())
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }

    private String catalogSummary(Order order) {
        if (order.getItems() == null) {
            return null;
        }
        String summary = order.getItems().stream()
                .map(this::itemName)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(10)
                .collect(Collectors.joining(", "));
        return StringUtils.hasText(summary) ? summary : null;
    }

    private String itemName(OrderItem item) {
        if (StringUtils.hasText(item.getProductNameSnapshot())) {
            return StringUtils.hasText(item.getVariantNameSnapshot())
                    ? item.getProductNameSnapshot() + " - " + item.getVariantNameSnapshot()
                    : item.getProductNameSnapshot();
        }
        if (item.getProductVariant() == null) {
            return null;
        }
        String variant = item.getProductVariant().getVariantName();
        String product = item.getProductVariant().getProduct() == null
                ? null
                : item.getProductVariant().getProduct().getProductName();
        return StringUtils.hasText(product) && StringUtils.hasText(variant)
                ? product + " - " + variant
                : StringUtils.hasText(product) ? product : variant;
    }

    private Integer totalQuantity(Order order) {
        return order.getItems() == null
                ? null
                : order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
    }

    private Integer maximumDurationDays(Order order) {
        return order.getItems() == null
                ? null
                : order.getItems().stream()
                        .map(OrderItem::getProductionDurationDays)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(null);
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
