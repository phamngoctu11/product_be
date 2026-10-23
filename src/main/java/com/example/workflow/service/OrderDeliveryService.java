package com.example.workflow.service;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.ReceiptConfirmRequest;
import com.example.workflow.dto.ReceiptConfirmResponse;
import com.example.workflow.dto.ReceiptItemDTO;
import com.example.workflow.dto.ReceiptMismatchDTO;
import com.example.workflow.dto.ReceiptViewDTO;
import com.example.workflow.dto.ShippingHandoverItemDTO;
import com.example.workflow.dto.ShippingHandoverItemRequest;
import com.example.workflow.dto.ShippingHandoverRequest;
import com.example.workflow.dto.ShippingHandoverResponse;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.OrderReceipt;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderDeliveredEvent;
import com.example.workflow.event.payload.OrderShippingEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderReceiptRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.GuestOrderAccessGuard;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderDeliveryService {
    private static final int DELIVERY_CONTRACT_VERSION = 1;
    private static final int EXPECTED_SHIPPING_DAYS = 2;
    private static final Duration GUEST_ACCESS_WINDOW = Duration.ofHours(1);
    private static final long GUEST_VIEW_LIMIT = 30;
    private static final long GUEST_CONFIRM_LIMIT = 8;

    private final OrderRepository orderRepository;
    private final OrderReceiptRepository receiptRepository;
    private final OrderAssignmentRepository assignmentRepository;
    private final CurrentUserService currentUserService;
    private final GuestOrderAccessGuard guestAccessGuard;
    private final OrderLookupTokenService tokenService;
    private final DurableRequestExecutor durableRequests;
    private final ReputationService reputationService;
    private final OrderStatusHistoryService historyService;
    private final DomainEventPublisher eventPublisher;
    private final ApplicationCacheService cacheService;
    private final ObjectMapper objectMapper;

    public ShippingHandoverResponse handover(
            Long orderId,
            ShippingHandoverRequest request,
            String idempotencyKey
    ) {
        User staff = currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.SHIPPING_HANDOVER_FORBIDDEN);
        requireActive(staff, ConstantErrorCode.SHIPPING_HANDOVER_FORBIDDEN);
        requireHandoverRequest(request);
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        canonical.put("orderVersion", request.orderVersion());
        canonical.put("items", request.items().stream()
                .map(item -> Map.of(
                        "orderItemId", item.orderItemId(),
                        "exportedQuantity", item.exportedQuantity()
                ))
                .sorted(java.util.Comparator.comparing(item -> (Long) item.get("orderItemId")))
                .toList());
        canonical.put("handoverNote", normalizeOptionalNote(request.handoverNote()));
        String result = durableRequests.execute(
                "shipping-handover:staff:" + staff.getId() + ":order:" + orderId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "shipping handover"),
                () -> JsonUtils.write(objectMapper,
                        handoverWithinTransaction(orderId, request, staff),
                        "shipping handover result")
        );
        return JsonUtils.read(objectMapper, result, ShippingHandoverResponse.class, "shipping handover result");
    }

    @Transactional(readOnly = true)
    public ReceiptViewDTO getUserReceiptView(Long orderId) {
        User owner = currentUserService.requireCurrentUser(Role.USER, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN);
        requireActive(owner, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN);
        Order order = requireOrder(orderId);
        requireOwner(order, owner.getId());
        return toReceiptView(order);
    }

    @Transactional(readOnly = true)
    public ReceiptViewDTO getGuestReceiptView(Long orderId, String rawToken) {
        Order order = requireOrder(orderId);
        guestAccessGuard.authorize(
                order, rawToken, OrderLookupTokenService.Scope.READ, GUEST_VIEW_LIMIT, GUEST_ACCESS_WINDOW
        );
        return toReceiptView(order);
    }

    public ReceiptConfirmResponse confirmByUser(
            Long orderId,
            ReceiptConfirmRequest request,
            String idempotencyKey
    ) {
        User owner = currentUserService.requireCurrentUser(Role.USER, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN);
        requireActive(owner, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN);
        requireReceiptRequest(request);
        return executeReceipt(
                "receipt:user:" + owner.getId() + ":order:" + orderId,
                idempotencyKey,
                orderId,
                request,
                owner.getId(),
                false,
                null
        );
    }

    public ReceiptConfirmResponse confirmByGuest(
            Long orderId,
            String rawToken,
            ReceiptConfirmRequest request,
            String idempotencyKey
    ) {
        requireReceiptRequest(request);
        Order visibleOrder = requireOrder(orderId);
        guestAccessGuard.authorize(
                visibleOrder, rawToken, OrderLookupTokenService.Scope.CONFIRM_RECEIPT,
                GUEST_CONFIRM_LIMIT, GUEST_ACCESS_WINDOW
        );
        return executeReceipt(
                "receipt:guest:order:" + orderId,
                idempotencyKey,
                orderId,
                request,
                null,
                true,
                rawToken
        );
    }

    private ReceiptConfirmResponse executeReceipt(
            String scope,
            String idempotencyKey,
            Long orderId,
            ReceiptConfirmRequest request,
            String ownerId,
            boolean guest,
            String rawToken
    ) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("orderId", orderId);
        canonical.put("orderVersion", request.getOrderVersion());
        canonical.put("items", request.getReceivedItems().stream()
                .map(item -> Map.of(
                        "orderItemId", item.getOrderItemId(),
                        "receivedQuantity", item.getReceivedQuantity()
                ))
                .sorted(java.util.Comparator.comparing(item -> (Long) item.get("orderItemId")))
                .toList());
        canonical.put("acceptMismatch", request.isAcceptMismatch());
        canonical.put("note", normalizeOptionalNote(request.getNote()));
        String result = durableRequests.execute(
                scope,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "receipt confirmation"),
                () -> JsonUtils.write(objectMapper,
                        confirmWithinTransaction(orderId, request, ownerId, guest, rawToken),
                        "receipt confirmation result")
        );
        return JsonUtils.read(objectMapper, result, ReceiptConfirmResponse.class, "receipt confirmation result");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ShippingHandoverResponse handoverWithinTransaction(
            Long orderId,
            ShippingHandoverRequest request,
            User staff
    ) {
        Order order = lockOrder(orderId);
        requireOrderVersion(order, request.orderVersion());
        requireHistoricalStaff(order, staff.getId());
        if (order.getStatus() != OrderStatus.READY_TO_SHIP) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_NOT_READY_TO_SHIP);
        }

        Map<Long, Integer> quantities = validateCompleteHandoverItems(order, request.items());
        safeItems(order).forEach(item -> item.setExportedQuantity(quantities.get(item.getId())));
        LocalDateTime now = LocalDateTime.now();
        OrderStatus oldStatus = order.getStatus();
        order.setDeliveryContractVersion(DELIVERY_CONTRACT_VERSION);
        order.setShippedById(staff.getId());
        order.setHandoverNote(normalizeOptionalNote(request.handoverNote()));
        order.setExpectedShippingDays(EXPECTED_SHIPPING_DAYS);
        order.setShippedAt(now);
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.saveAndFlush(order);
        historyService.record(order, oldStatus, OrderStatus.SHIPPING, staff.getId());
        eventPublisher.publishAfterCommit(EventTypes.ORDER_SHIPPING,
                new OrderShippingEvent(order.getId(), staff.getId()));
        cacheService.evictShippingHandover(order, staff.getId());
        return toHandoverResponse(order);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ReceiptConfirmResponse confirmWithinTransaction(
            Long orderId,
            ReceiptConfirmRequest request,
            String ownerId,
            boolean guest,
            String rawToken
    ) {
        Order order = lockOrder(orderId);
        if (guest) {
            tokenService.authorize(order, rawToken, OrderLookupTokenService.Scope.CONFIRM_RECEIPT);
        } else {
            requireOwner(order, ownerId);
        }
        requireOrderVersion(order, request.getOrderVersion());
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_NOT_AWAITING_RECEIPT_CONFIRMATION);
        }

        Map<Long, Integer> received = validateCompleteReceiptItems(order, request.getReceivedItems());
        List<ReceiptMismatchDTO> mismatches = buildMismatches(order, received);
        boolean matched = mismatches.isEmpty();
        if (!matched && !request.isAcceptMismatch()) {
            return new ReceiptConfirmResponse(
                    order.getId(), order.getVersion(), order.getStatus(), false, false, false, null,
                    "Số lượng thực nhận không khớp với số lượng đã bàn giao vận chuyển.", mismatches
            );
        }

        safeItems(order).forEach(item -> item.setReceivedQuantity(received.get(item.getId())));
        LocalDateTime now = LocalDateTime.now();
        OrderStatus oldStatus = order.getStatus();
        OrderReceipt receipt = new OrderReceipt();
        receipt.setOrderId(order.getId());
        receipt.setConfirmedBy(guest ? null : ownerId);
        receipt.setGuestTokenReference(guest
                ? DurableRequestExecutor.hash("guest-receipt:" + order.getId() + ":" + rawToken)
                : null);
        receipt.setConfirmedAt(now);
        receipt.setMismatchAccepted(!matched);
        receipt.setItemsSnapshot(receiptSnapshot(order, received, matched, request));
        receiptRepository.saveAndFlush(receipt);
        order.setDeliveredAt(now);
        order.setEndOrderTime(now);
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.saveAndFlush(order);

        if (!guest) {
            reputationService.changeReputation(
                    order.getUser(), 2, "Completed order #" + order.getId(), "ORDER_DELIVERED",
                    String.valueOf(order.getId())
            );
        }
        historyService.record(order, oldStatus, OrderStatus.DELIVERED, guest ? null : ownerId);
        eventPublisher.publishAfterCommit(EventTypes.ORDER_DELIVERED, new OrderDeliveredEvent(order.getId()));
        cacheService.evictOrderDelivered(order, ownerId);
        return new ReceiptConfirmResponse(
                order.getId(), order.getVersion(), order.getStatus(), matched, true, !matched, now,
                matched
                        ? "Xác nhận nhận hàng thành công."
                        : "Đã xác nhận nhận hàng và chấp nhận chênh lệch số lượng.",
                mismatches
        );
    }

    private Map<Long, Integer> validateCompleteHandoverItems(
            Order order,
            List<ShippingHandoverItemRequest> requestedItems
    ) {
        Map<Long, OrderItem> orderItems = itemMap(order);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (ShippingHandoverItemRequest requested : requestedItems) {
            if (requested == null || requested.orderItemId() == null || requested.exportedQuantity() == null) {
                throw invalidHandover("Every item requires orderItemId and exportedQuantity.");
            }
            OrderItem item = orderItems.get(requested.orderItemId());
            if (item == null) throw invalidHandover("Order item does not belong to this order: " + requested.orderItemId());
            if (requested.exportedQuantity() < 0 || requested.exportedQuantity() > item.getQuantity()) {
                throw invalidHandover("Exported quantity must be between 0 and ordered quantity for item " + item.getId() + ".");
            }
            if (result.put(item.getId(), requested.exportedQuantity()) != null) {
                throw invalidHandover("Duplicate order item: " + item.getId() + ".");
            }
        }
        if (!result.keySet().equals(orderItems.keySet())) {
            throw invalidHandover("Every order item must appear exactly once.");
        }
        return result;
    }

    private Map<Long, Integer> validateCompleteReceiptItems(Order order, List<ItemCheckRequest> requestedItems) {
        Map<Long, OrderItem> orderItems = itemMap(order);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (ItemCheckRequest requested : requestedItems) {
            if (requested == null || requested.getOrderItemId() == null) {
                throw invalidReceipt("Every item requires orderItemId and receivedQuantity.");
            }
            OrderItem item = orderItems.get(requested.getOrderItemId());
            if (item == null) throw invalidReceipt("Order item does not belong to this order: " + requested.getOrderItemId());
            if (requested.getReceivedQuantity() < 0) {
                throw invalidReceipt("Received quantity cannot be negative for item " + item.getId() + ".");
            }
            effectiveExportedQuantity(order, item);
            if (result.put(item.getId(), requested.getReceivedQuantity()) != null) {
                throw invalidReceipt("Duplicate order item: " + item.getId() + ".");
            }
        }
        if (!result.keySet().equals(orderItems.keySet())) {
            throw invalidReceipt("Every order item must appear exactly once.");
        }
        return result;
    }

    private List<ReceiptMismatchDTO> buildMismatches(Order order, Map<Long, Integer> received) {
        List<ReceiptMismatchDTO> mismatches = new ArrayList<>();
        for (OrderItem item : safeItems(order)) {
            int exported = effectiveExportedQuantity(order, item);
            int actual = received.get(item.getId());
            if (actual != exported) {
                ProductVariant variant = item.getProductVariant();
                mismatches.add(new ReceiptMismatchDTO(
                        item.getId(), variant == null ? null : variant.getId(), item.getProductNameSnapshot(),
                        item.getVariantNameSnapshot(), item.getQuantity(), exported, actual
                ));
            }
        }
        return mismatches;
    }

    private String receiptSnapshot(
            Order order,
            Map<Long, Integer> received,
            boolean matched,
            ReceiptConfirmRequest request
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("matched", matched);
        snapshot.put("mismatchAccepted", !matched);
        snapshot.put("note", normalizeOptionalNote(request.getNote()));
        snapshot.put("items", safeItems(order).stream().map(item -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("orderItemId", item.getId());
            values.put("orderedQuantity", item.getQuantity());
            values.put("exportedQuantity", effectiveExportedQuantity(order, item));
            values.put("receivedQuantity", received.get(item.getId()));
            return values;
        }).toList());
        return JsonUtils.write(objectMapper, snapshot, "receipt snapshot");
    }

    private int effectiveExportedQuantity(Order order, OrderItem item) {
        if (item.getExportedQuantity() != null) return item.getExportedQuantity();
        if (order.getDeliveryContractVersion() == null) return item.getQuantity();
        throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.RECEIPT_HANDOVER_DATA_MISSING);
    }

    private ReceiptViewDTO toReceiptView(Order order) {
        boolean legacyFallback = order.getDeliveryContractVersion() == null
                && safeItems(order).stream().anyMatch(item -> item.getExportedQuantity() == null);
        boolean handoverComplete = safeItems(order).stream().allMatch(item ->
                item.getExportedQuantity() != null || order.getDeliveryContractVersion() == null);
        List<ReceiptItemDTO> items = safeItems(order).stream().map(item -> {
            ProductVariant variant = item.getProductVariant();
            Integer exported = item.getExportedQuantity();
            if (exported == null && order.getDeliveryContractVersion() == null) exported = item.getQuantity();
            return new ReceiptItemDTO(
                    item.getId(), variant == null ? null : variant.getId(), item.getProductNameSnapshot(),
                    item.getVariantNameSnapshot(), item.getQuantity(), exported, item.getReceivedQuantity()
            );
        }).toList();
        return new ReceiptViewDTO(
                order.getId(), order.getVersion(), order.getOrderType(), order.getStatus(), order.getShippedAt(),
                order.getExpectedShippingDays(), expectedDeliveryAt(order), order.getDeliveredAt(),
                order.getStatus() == OrderStatus.SHIPPING && handoverComplete,
                legacyFallback ? "LEGACY_EXPORTED_QUANTITY_FALLBACK" : null,
                items
        );
    }

    private ShippingHandoverResponse toHandoverResponse(Order order) {
        return new ShippingHandoverResponse(
                order.getId(), order.getVersion(), order.getStatus(), order.getShippedAt(),
                order.getExpectedShippingDays(), expectedDeliveryAt(order),
                safeItems(order).stream()
                        .map(item -> new ShippingHandoverItemDTO(
                                item.getId(), item.getQuantity(), item.getExportedQuantity()))
                        .toList()
        );
    }

    private LocalDateTime expectedDeliveryAt(Order order) {
        return order.getShippedAt() == null ? null : order.getShippedAt().plusDays(order.getExpectedShippingDays());
    }

    private Map<Long, OrderItem> itemMap(Order order) {
        if (safeItems(order).isEmpty()) throw invalidReceipt("Order has no items.");
        return safeItems(order).stream().collect(Collectors.toMap(
                OrderItem::getId, Function.identity(),
                (left, right) -> { throw invalidReceipt("Duplicate persisted order item id."); },
                LinkedHashMap::new
        ));
    }

    private void requireHistoricalStaff(Order order, String staffId) {
        Set<String> historicalStaff = assignmentRepository.findByOrderIdOrderByAssignedAtDesc(order.getId()).stream()
                .map(OrderAssignment::getStaffId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (order.getAssignedStaff() != null) historicalStaff.add(order.getAssignedStaff().getId());
        if (order.getWarehouseStaff() != null) historicalStaff.add(order.getWarehouseStaff().getId());
        if (!historicalStaff.contains(staffId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.SHIPPING_HANDOVER_FORBIDDEN);
        }
    }

    private void requireHandoverRequest(ShippingHandoverRequest request) {
        if (request == null || request.orderVersion() == null || request.items() == null || request.items().isEmpty()) {
            throw invalidHandover("orderVersion and a complete item list are required.");
        }
    }

    private void requireReceiptRequest(ReceiptConfirmRequest request) {
        if (request == null || request.getOrderVersion() == null
                || request.getReceivedItems() == null || request.getReceivedItems().isEmpty()) {
            throw invalidReceipt("orderVersion and a complete received item list are required.");
        }
    }

    private String normalizeOptionalNote(String note) {
        if (!StringUtils.hasText(note)) return null;
        String normalized = note.trim();
        if (normalized.length() > 2000) throw invalidReceipt("Note must be at most 2000 characters.");
        return normalized;
    }

    private void requireActive(User actor, ConstantErrorCode errorCode) {
        if (actor == null || actor.isDelete() || !Boolean.TRUE.equals(actor.getIsActive())) {
            throw new AppException(HttpStatus.FORBIDDEN, errorCode);
        }
    }

    private void requireOwner(Order order, String ownerId) {
        if (order.getUser() == null || !Objects.equals(order.getUser().getId(), ownerId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN);
        }
    }

    private void requireOrderVersion(Order order, Long expectedVersion) {
        if (!Objects.equals(order.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_VERSION_CONFLICT);
        }
    }

    private Order lockOrder(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
    }

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
    }

    private List<OrderItem> safeItems(Order order) {
        return order.getItems() == null ? List.of() : order.getItems();
    }

    private AppException invalidHandover(String detail) {
        return new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.SHIPPING_HANDOVER_INVALID, detail);
    }

    private AppException invalidReceipt(String detail) {
        return new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.RECEIPT_CONFIRMATION_INVALID, detail);
    }
}
