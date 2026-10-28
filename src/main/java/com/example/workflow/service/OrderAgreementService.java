package com.example.workflow.service;

import com.example.workflow.dto.OrderAgreementDTO;
import com.example.workflow.dto.OrderAgreementDecisionRequest;
import com.example.workflow.dto.SubmitOrderAgreementRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAgreement;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderAcceptedEvent;
import com.example.workflow.event.payload.OrderAgreementSubmittedEvent;
import com.example.workflow.event.payload.OrderAgreementReviewedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderAgreementDecision;
import com.example.workflow.nume.OrderAgreementStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentMethod;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.OrderAgreementRepository;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.example.workflow.util.TextNormalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderAgreementService {
    private final OrderRepository orderRepository;
    private final OrderLookupService orderLookupService;
    private final OrderAgreementRepository agreementRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final CurrentUserService currentUserService;
    private final DurableRequestExecutor durableRequests;
    private final OrderTransitionService transitionService;
    private final OrderStatusHistoryService historyService;
    private final VoucherService voucherService;
    private final NotificationService notificationService;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public OrderAgreementDTO getForCurrentParticipant(Long orderId) {
        Order order = orderLookupService.require(orderId);
        authorizeParticipant(order, currentUserService.requireCurrentUserId());
        return agreementRepository.findFirstByOrderIdOrderByAgreementVersionDesc(orderId)
                .map(agreement -> toDto(order, agreement))
                .orElseGet(() -> emptyDto(order));
    }

    public OrderAgreementDTO submit(Long orderId, SubmitOrderAgreementRequest request, String idempotencyKey) {
        String userId = currentUserService.requireCurrentUserId();
        String canonical = JsonUtils.write(objectMapper, request, "order agreement submission");
        String result = durableRequests.execute(
                "order-agreement:submit:" + userId + ":" + orderId,
                idempotencyKey,
                canonical,
                () -> JsonUtils.write(objectMapper, submitWithinTransaction(orderId, request, userId), "order agreement result")
        );
        return JsonUtils.read(objectMapper, result, OrderAgreementDTO.class, "order agreement result");
    }

    public OrderAgreementDTO decide(Long orderId, OrderAgreementDecisionRequest request, String idempotencyKey) {
        String staffId = currentUserService.requireCurrentUserId();
        String canonical = JsonUtils.write(objectMapper, request, "order agreement decision");
        String result = durableRequests.execute(
                "order-agreement:decision:" + staffId + ":" + orderId,
                idempotencyKey,
                canonical,
                () -> JsonUtils.write(objectMapper, decideWithinTransaction(orderId, request, staffId), "order agreement result")
        );
        return JsonUtils.read(objectMapper, result, OrderAgreementDTO.class, "order agreement result");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    OrderAgreementDTO submitWithinTransaction(Long orderId, SubmitOrderAgreementRequest request, String userId) {
        if (request == null || request.orderVersion() == null) {
            throw badRequest("Phiên bản đơn hàng là bắt buộc..");
        }
        Order order = orderLookupService.requireForUpdate(orderId);
        requireOwner(order, userId);
        requireActiveAssignment(order, null);
        requireVersion(order, request.orderVersion());
        if (order.getStatus() != OrderStatus.DISCUSSING) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_TRANSITION_INVALID);
        }
        if (order.getOrderType() == OrderType.CUSTOM
                && !agreementRepository.existsByOrderIdAndSubmittedAtIsNotNull(orderId)
                && order.getConfirmationDueAt() != null && !LocalDateTime.now().isBefore(order.getConfirmationDueAt())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_TRANSITION_INVALID);
        }

        SnapshotAndTotals data = order.getOrderType() == OrderType.CUSTOM
                ? customSnapshot(order, request, userId)
                : catalogSnapshot(order, request);
        int agreementVersion = order.getCurrentAgreementVersion() == null ? 1 : order.getCurrentAgreementVersion() + 1;
        LocalDateTime now = LocalDateTime.now();

        OrderAgreement agreement = new OrderAgreement();
        agreement.setOrderId(orderId);
        agreement.setAgreementVersion(agreementVersion);
        agreement.setStatus(OrderAgreementStatus.USER_SUBMITTED);
        agreement.setItemSnapshot(data.snapshot());
        agreement.setSubtotal(data.subtotal());
        agreement.setDiscountAmount(data.discount());
        agreement.setFinalPrice(data.finalPrice());
        agreement.setUserVoucherId(data.userVoucherId());
        agreement.setSubmittedBy(userId);
        agreement.setSubmittedAt(now);
        agreementRepository.save(agreement);

        OrderStatus oldStatus = order.getStatus();
        Order transitioned = transitionService.apply(
                orderId, request.orderVersion(), OrderStatus.WAITING_STAFF_CONFIRMATION,
                userId, "User submitted agreement", "agreement:" + orderId + ":" + agreementVersion, null
        );
        transitioned.setCurrentAgreementVersion(agreementVersion);
        transitioned = orderRepository.saveAndFlush(transitioned);
        historyService.record(transitioned, oldStatus, transitioned.getStatus(), userId);
        String staffId = assignedStaffId(transitioned);
        notificationService.sendNotification(
                "Có thỏa thuận mới cần xác nhận",
                "Khách hàng đã gửi thỏa thuận phiên bản " + agreementVersion + " cho đơn #" + orderId + ".",
                orderId, staffId, "/topic/user-notifications/" + staffId
        );
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_AGREEMENT_SUBMITTED,
                new OrderAgreementSubmittedEvent(orderId, agreementVersion, data.paymentMethod())
        );
        return toDto(transitioned, agreement);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    OrderAgreementDTO decideWithinTransaction(Long orderId, OrderAgreementDecisionRequest request, String staffId) {
        if (request == null || request.orderVersion() == null || request.agreementVersion() == null || request.decision() == null) {
            throw badRequest("Vui lòng cung cấp phiên bản đơn hàng, phiên bản thỏa thuận và quyết định.");
        }
        Order order = orderLookupService.requireForUpdate(orderId);
        requireActiveAssignment(order, staffId);
        requireVersion(order, request.orderVersion());
        if (order.getStatus() != OrderStatus.WAITING_STAFF_CONFIRMATION
                || !Objects.equals(order.getCurrentAgreementVersion(), request.agreementVersion())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_TRANSITION_INVALID);
        }
        OrderAgreement agreement = agreementRepository
                .findByOrderIdAndAgreementVersionForUpdate(orderId, request.agreementVersion())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.BAD_REQUEST_DETAIL, "Không tìm thấy thỏa thuận."));
        if (agreement.getStatus() != OrderAgreementStatus.USER_SUBMITTED) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_TRANSITION_INVALID);
        }

        if (request.decision() == OrderAgreementDecision.REQUEST_CHANGES) {
            String reason = normalizedReason(request.reason());
            agreement.setStatus(OrderAgreementStatus.CHANGES_REQUESTED);
            agreement.setDecisionReason(reason);
            agreement.setConfirmedBy(staffId);
            agreement.setConfirmedAt(LocalDateTime.now());
            agreementRepository.save(agreement);
            Order transitioned = transitionService.apply(
                    orderId, request.orderVersion(), OrderStatus.DISCUSSING, staffId, reason,
                    "agreement-changes:" + orderId + ":" + agreement.getAgreementVersion(), null
            );
            historyService.record(transitioned, OrderStatus.WAITING_STAFF_CONFIRMATION, transitioned.getStatus(), staffId);
            notifyOwner(transitioned, "Nhân viên yêu cầu chỉnh sửa thỏa thuận", reason);
            eventPublisher.publishAfterCommit(
                    EventTypes.ORDER_AGREEMENT_REVIEWED,
                    new OrderAgreementReviewedEvent(orderId, agreement.getAgreementVersion(), request.decision(), staffId)
            );
            return toDto(transitioned, agreement);
        }

        agreement.setStatus(OrderAgreementStatus.STAFF_CONFIRMED);
        agreement.setDecisionReason(TextNormalizer.optional(request.reason()));
        agreement.setConfirmedBy(staffId);
        agreement.setConfirmedAt(LocalDateTime.now());
        agreementRepository.save(agreement);
        Order transitioned = transitionService.apply(
                orderId, request.orderVersion(), OrderStatus.ORDER_ACCEPTED, staffId, "Staff confirmed agreement",
                "agreement-confirm:" + orderId + ":" + agreement.getAgreementVersion(), null
        );
        applyOfficialAgreement(transitioned, agreement);
        transitioned = orderRepository.saveAndFlush(transitioned);
        historyService.record(transitioned, OrderStatus.WAITING_STAFF_CONFIRMATION, transitioned.getStatus(), staffId);
        eventPublisher.publishAfterCommit(EventTypes.ORDER_ACCEPTED, new OrderAcceptedEvent(orderId));
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_AGREEMENT_REVIEWED,
                new OrderAgreementReviewedEvent(orderId, agreement.getAgreementVersion(), request.decision(), staffId)
        );
        return toDto(transitioned, agreement);
    }

    private SnapshotAndTotals customSnapshot(Order order, SubmitOrderAgreementRequest request, String userId) {
        String description = normalizedDescription(request.description());
        if (request.quantity() == null || request.quantity() < 1 || request.unitPrice() == null
                || request.unitPrice().signum() <= 0 || request.productionDurationDays() == null
                || request.productionDurationDays() < 1) {
            throw badRequest("Thỏa thuận tùy chỉnh cần có số lượng, đơn giá dương và thời gian sản xuất.");
        }
        PaymentMethod paymentMethod = requireCustomPaymentMethod(order, request.paymentMethod());
        List<String> imageUrls = validateImageUrls(request.imageUrls());
        BigDecimal subtotal = request.unitPrice().multiply(BigDecimal.valueOf(request.quantity())).setScale(2, RoundingMode.HALF_UP);
        UserVoucher voucher = voucherService.previewVoucherForCheckout(request.userVoucherId(), userId, subtotal);
        BigDecimal discount = voucherService.calculateDiscountAmount(voucher, subtotal);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        OrderItem item = order.itemsOrEmpty().stream().findFirst().orElse(null);
        if (item != null && StringUtils.hasText(item.getSpecSnapshot())) {
            try {
                JsonNode original = objectMapper.readTree(item.getSpecSnapshot());
                if (original != null && original.isObject()) {
                    for (String key : List.of("productType", "productTypeFields", "fieldValues")) {
                        JsonNode value = original.get(key);
                        if (value != null) {
                            snapshot.put(key, objectMapper.convertValue(value, Object.class));
                        }
                    }
                }
            } catch (JsonProcessingException ignored) {
                // Older custom orders may store a plain-text specification instead of a JSON snapshot.
            }
        }
        snapshot.put("description", description);
        snapshot.put("imageUrls", imageUrls);
        snapshot.put("quantity", request.quantity());
        snapshot.put("unitPrice", request.unitPrice().setScale(2, RoundingMode.HALF_UP));
        snapshot.put("productionDurationDays", request.productionDurationDays());
        snapshot.put("paymentMethod", paymentMethod.name());
        snapshot.put("note", TextNormalizer.optional(request.note()));
        return new SnapshotAndTotals(
                JsonUtils.write(objectMapper, snapshot, "custom agreement snapshot"),
                subtotal, discount, floorZero(subtotal.subtract(discount)),
                request.userVoucherId(), paymentMethod
        );
    }

    private SnapshotAndTotals catalogSnapshot(Order order, SubmitOrderAgreementRequest request) {
        if (StringUtils.hasText(request.description()) || request.quantity() != null || request.unitPrice() != null
                || request.productionDurationDays() != null || (request.imageUrls() != null && !request.imageUrls().isEmpty())) {
            throw badRequest("Chỉ được xem sản phẩm danh mục, số lượng, giá và thời gian sản xuất.");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem item : order.itemsOrEmpty()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("orderItemId", item.getId());
            value.put("productName", item.getProductNameSnapshot());
            value.put("variantName", item.getVariantNameSnapshot());
            value.put("quantity", item.getQuantity());
            value.put("unitPrice", item.getPrice());
            value.put("productionDurationDays", item.getProductionDurationDays());
            items.add(value);
        }
        BigDecimal subtotal = nonNegative(order.getTotalPrice(), "Tạm tính của sản phẩm danh mục không hợp lệ.");
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : nonNegative(order.getDiscountAmount(), "Mức giảm giá của sản phẩm danh mục không hợp lệ.");
        BigDecimal finalPrice = order.getFinalPrice() == null ? floorZero(subtotal.subtract(discount))
                : nonNegative(order.getFinalPrice(), "Giá cuối cùng của sản phẩm danh mục không hợp lệ.");
        Long voucherId = order.getUserVoucher() == null ? null : order.getUserVoucher().getId();
        PaymentMethod paymentMethod = order.getPaymentMethodType();
        if (request.paymentMethod() != null && request.paymentMethod() != paymentMethod) {
            throw badRequest("Không thể thay đổi phương thức thanh toán của sản phẩm danh mục trong thỏa thuận.");
        }
        if (request.userVoucherId() != null && !Objects.equals(request.userVoucherId(), voucherId)) {
            throw badRequest("Không thể thay đổi mã giảm giá của sản phẩm danh mục trong thỏa thuận.");
        }
        return new SnapshotAndTotals(
                JsonUtils.write(objectMapper, items, "catalog agreement snapshot"),
                subtotal, discount, finalPrice, voucherId, paymentMethod
        );
    }

    @SuppressWarnings("unchecked")
    private void applyOfficialAgreement(Order order, OrderAgreement agreement) {
        if (order.getOrderType() == OrderType.CATALOG) {
            return;
        }
        Map<String, Object> snapshot = JsonUtils.read(objectMapper, agreement.getItemSnapshot(), Map.class, "custom agreement snapshot");
        List<OrderItem> items = order.itemsOrEmpty();
        if (items.isEmpty()) {
            throw badRequest("Đơn hàng tùy chỉnh không có sản phẩm.");
        }
        OrderItem item = items.get(0);
        int quantity = ((Number) snapshot.get("quantity")).intValue();
        BigDecimal unitPrice = new BigDecimal(String.valueOf(snapshot.get("unitPrice"))).setScale(2, RoundingMode.HALF_UP);
        int duration = ((Number) snapshot.get("productionDurationDays")).intValue();
        UserVoucher voucher = voucherService.useVoucherForCheckout(
                agreement.getUserVoucherId(), order.getUser().getId(), agreement.getSubtotal()
        );
        item.setQuantity(quantity);
        item.setPrice(unitPrice);
        item.setProductionDurationDays(duration);
        item.setSpecSnapshot(agreement.getItemSnapshot());
        order.setTotalPrice(agreement.getSubtotal());
        order.setDiscountAmount(agreement.getDiscountAmount());
        order.setFinalPrice(agreement.getFinalPrice());
        order.setUserVoucher(voucher);
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(String.valueOf(snapshot.get("paymentMethod")));
        } catch (IllegalArgumentException exception) {
            throw badRequest("Phương thức thanh toán trong thỏa thuận tùy chỉnh không hợp lệ.");
        }
        order.setPaymentMethodType(paymentMethod);
        order.setPaymentMethod(paymentMethod.name());
        order.setPaymentStatus(PaymentStatus.NOT_DUE);
    }

    private void authorizeParticipant(Order order, String actorId) {
        if (order.getUser() == null || (!actorId.equals(order.getUser().getId()) && !actorId.equals(assignedStaffId(order)))) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
        requireActiveAssignment(order, actorId.equals(order.getUser().getId()) ? null : actorId);
    }

    private void requireOwner(Order order, String userId) {
        if (order.getUser() == null || !userId.equals(order.getUser().getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
    }

    private void requireActiveAssignment(Order order, String expectedStaffId) {
        OrderAssignment assignment = assignmentRepository.findActiveByOrderId(order.getId())
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_HAS_NO_ASSIGNED_STAFF));
        if (!Objects.equals(assignment.getStaffId(), assignedStaffId(order))
                || (expectedStaffId != null && !expectedStaffId.equals(assignment.getStaffId()))) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
    }

    private void requireVersion(Order order, Long expectedVersion) {
        if (!Objects.equals(order.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        }
    }

    private OrderAgreementDTO toDto(Order order, OrderAgreement agreement) {
        return new OrderAgreementDTO(
                agreement.getId(), order.getId(), order.getVersion(), order.getOrderType(), order.getStatus(),
                agreement.getAgreementVersion(), agreement.getStatus(), agreement.getItemSnapshot(),
                agreement.getSubtotal(), agreement.getDiscountAmount(), agreement.getFinalPrice(),
                agreement.getUserVoucherId(), agreement.getSubmittedBy(), agreement.getSubmittedAt(),
                agreement.getConfirmedBy(), agreement.getConfirmedAt(), agreement.getDecisionReason(), order.getConfirmationDueAt()
        );
    }

    private OrderAgreementDTO emptyDto(Order order) {
        return new OrderAgreementDTO(null, order.getId(), order.getVersion(), order.getOrderType(), order.getStatus(),
                null, null, null, null, null, null, null, null, null, null, null, null, order.getConfirmationDueAt());
    }

    private void notifyOwner(Order order, String title, String content) {
        String ownerId = order.getUser().getId();
        notificationService.sendNotification(title, content, order.getId(), ownerId, "/topic/user-notifications/" + ownerId);
    }

    private String assignedStaffId(Order order) {
        if (order.getAssignedStaff() != null) return order.getAssignedStaff().getId();
        return order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
    }

    private String normalizedDescription(String value) {
        if (!StringUtils.hasText(value)) throw badRequest("Vui lòng nhập mô tả yêu cầu tùy chỉnh.");
        String normalized = value.trim();
        if (normalized.length() > 4000) throw badRequest("Mô tả yêu cầu tùy chỉnh không được vượt quá 4000 ký tự.");
        return normalized;
    }

    private List<String> validateImageUrls(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(value -> {
            try {
                URI uri = URI.create(value == null ? "" : value.trim());
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                    throw new IllegalArgumentException();
                }
                return uri.toString();
            } catch (IllegalArgumentException exception) {
                throw badRequest("Đường dẫn hình ảnh phải sử dụng giao thức HTTP hoặc HTTPS.");
            }
        }).toList();
    }

    private PaymentMethod requireCustomPaymentMethod(Order order, PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw badRequest("Vui lòng chọn phương thức thanh toán cho thỏa thuận tùy chỉnh.");
        }
        if (paymentMethod == PaymentMethod.COD
                && (order.getUser() == null || order.getUser().getReputation() < 20)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.LOW_REPUTATION_REQUIRES_ONLINE_PAYMENT);
        }
        return paymentMethod;
    }

    private String normalizedReason(String value) {
        if (!StringUtils.hasText(value)) throw badRequest("A Vui lòng nhập lý do. when requesting changes.");
        return value.trim();
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) throw badRequest(message);
        return value;
    }

    private BigDecimal floorZero(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, message);
    }

    private record SnapshotAndTotals(
            String snapshot,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal finalPrice,
            Long userVoucherId,
            PaymentMethod paymentMethod
    ) {
    }
}
