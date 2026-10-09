package com.example.workflow.service;

import com.example.workflow.dto.*;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.entity.*;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderDeliveredEvent;
import com.example.workflow.event.payload.PaymentConfirmedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.mapper.OrderStatusHistoryMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.ProductAvailabilityStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.*;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.assembler.OrderDetailsAssembler;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderStatusHistoryMapper historyMapper;
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final DomainEventPublisher eventPublisher;
    private final InventoryReservationService inventoryReservationService;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final ReputationService reputationService;
    private final CartService cartService;
    private final OrderDetailsAssembler orderDetailsAssembler;
    private final ApplicationCacheService applicationCacheService;
    private final OrderLookupService orderLookupService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final OrderCancellationService orderCancellationService;

    private void assertCurrentUserCanViewOrder(Order order) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == Role.USER
                && (order.getUser() == null || !order.getUser().getId().equals(currentUser.getId()))) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
    }

    private Task findWorkflowTask(Long orderId, String taskDefinitionKey, String missingMessage) {
        Task task = queryWorkflowTask(orderId, taskDefinitionKey);
        if (task == null) {
            throw new RuntimeException(missingMessage);
        }
        return task;
    }

    private Task queryWorkflowTask(Long orderId, String taskDefinitionKey) {
        return taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult();
    }

    private void saveOrderAndAuditStatusChange(Order order, OrderStatus oldStatus, String changerId) {
        orderRepository.save(order);
        if (oldStatus != order.getStatus()) {
            orderStatusHistoryService.record(order, oldStatus, order.getStatus(), changerId);
        }
    }

    private OrderItem findOrderItemByVariant(Order order, Long variantId) {
        return order.getItems().stream()
                .filter(existingItem -> existingItem.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VARIANT_NOT_IN_ORDER, variantId));
    }

    // ==========================================
    // Station 2: warehouse export
    // ==========================================
    @Transactional
    public void processStaffExport(Long orderId, List<ItemCheckRequest> exportData) {
        Order order = orderLookupService.require(orderId);
        User staff = currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);

        if (order.getStatus() != OrderStatus.ORDER_ACCEPTED) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_STAFF_REQUIRED_BEFORE_EXPORT);
        }
        if (order.getWarehouseStaff() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_HAS_NO_ASSIGNED_STAFF);
        }
        if (!order.getWarehouseStaff().getId().equals(staff.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.ONLY_ASSIGNED_STAFF_CAN_EXPORT);
        }
        if (exportData == null || exportData.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.EXPORT_DATA_REQUIRED);
        }

        Task task = findWorkflowTask(orderId, "staff_export_warehouse", "Order is not waiting for warehouse export!");

        // Apply actual exported quantities.
        for (ItemCheckRequest req : exportData) {
            if (req.getQuantity() < 0) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.EXPORT_QUANTITY_NEGATIVE);
            }
            OrderItem item = findOrderItemByVariant(order, req.getVariantId());
            item.setExportedQuantity(req.getQuantity());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.READY_TO_SHIP);
        saveOrderAndAuditStatusChange(order, oldStatus, staff.getId());
        taskService.complete(task.getId());

        applicationCacheService.evictStaffExported(order, staff.getId());
    }

    // ==========================================
    // Station 3: manager KCS reconciliation
    // ==========================================
    @Transactional
    public void processManagerKcsCheck(Long orderId, boolean isPassed,String cancelReason) {
        Order order = orderLookupService.require(orderId);

        if (order.getStatus() != OrderStatus.READY_TO_SHIP) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_NOT_WAITING_FOR_KCS);
        }

        Task task = findWorkflowTask(orderId, "manager_kcs_check", "KCS task not found for this order!");

        Map<String, Object> variables = new HashMap<>();
        variables.put("kcsPassed", isPassed);

        OrderStatus oldStatus = order.getStatus();
        if (isPassed) {
            order.setStatus(OrderStatus.SHIPPING);
            saveCustomerNotificationIfSystemUser(order, "Don hang dang giao", "Don hang #" + orderId + " da xuat kho va dang tren duong giao den ban.");
        } else {
            order.setStatus(OrderStatus.READY_TO_SHIP);
            String message = " vui long kiem tra lai so luong xuat.";
            if (cancelReason != null) message = "Ly do: " + cancelReason;
            String staffId = order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
            String destination = staffId == null ? "/topic/admin-notifications" : "/topic/user-notifications/" + staffId;
            saveAndSendNotification("Canh bao KCS", "Don # bi KCS danh rot" + orderId + message, orderId, staffId, destination);
        }

        saveOrderAndAuditStatusChange(order, oldStatus, null);
        taskService.complete(task.getId(), variables);

        applicationCacheService.evictManagerKcsChecked(order);
    }

    // ==========================================
    // Station 4: customer receipt confirmation
    // ==========================================
    @Transactional
    public ReceiptConfirmResponse confirmCustomerReceipt(Long orderId, ReceiptConfirmRequest request) {
        Order order = orderLookupService.require(orderId);
        User currentUser = validateReceiptOwner(order);
        Task task = findCustomerReceiptTask(orderId);

        Map<Long, Integer> receivedByVariant = buildReceivedQuantityMap(order, request.getReceivedItems());
        List<ReceiptMismatchDTO> mismatches = buildReceiptMismatches(order, receivedByVariant);
        if (!mismatches.isEmpty() && !request.isAcceptMismatch()) {
            return new ReceiptConfirmResponse(
                    false,
                    false,
                    "So luong thuc nhan khong khop voi so luong da xuat. Vui long xac nhan co muon khieu nai hay khong.",
                    mismatches
            );
        }

        applyReceivedQuantities(order, receivedByVariant);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);
        order.setEndOrderTime(LocalDateTime.now());

        reputationService.changeReputation(
                currentUser,
                2,
                "Completed order #" + order.getId(),
                "ORDER",
                String.valueOf(order.getId())
        );
        saveOrderAndAuditStatusChange(order, oldStatus, currentUser.getId());
        eventPublisher.publishAfterCommit(EventTypes.ORDER_DELIVERED, new OrderDeliveredEvent(order.getId()));

        taskService.complete(task.getId());

        boolean matched = mismatches.isEmpty();
        String message = matched
                ? "Xac nhan nhan hang thanh cong. Cam on ban!"
                : "Xac nhan nhan hang thanh cong voi so luong thuc nhan bi lech da duoc chap nhan.";
        return new ReceiptConfirmResponse(matched, true, message, mismatches);
    }

    @Transactional
    public ReceiptConfirmResponse sendReceiptComplaint(Long orderId, ReceiptComplaintRequest request) {
        Order order = orderLookupService.require(orderId);
        User currentUser = validateReceiptOwner(order);
        findCustomerReceiptTask(orderId);

        Map<Long, Integer> receivedByVariant = buildReceivedQuantityMap(order, request.getReceivedItems());
        List<ReceiptMismatchDTO> mismatches = buildReceiptMismatches(order, receivedByVariant);
        if (mismatches.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.RECEIVED_QUANTITY_MATCHES_EXPORTED);
        }

        List<String> managerEmails = userRepository.findByRoleInAndIsDeleteFalseAndEmailIsNotNull(List.of(Role.MANAGER))
                .stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (managerEmails.isEmpty()) {
            log.warn("No manager email found for receipt complaint on order {}; storing notification only.", orderId);
        } else {
            emailService.sendReceiptComplaintEmail(
                    managerEmails,
                    orderId,
                    userService.fullName(currentUser),
                    currentUser.getEmail(),
                    request.getNote(),
                    mismatches
            );
        }

        saveAndSendNotification(
                "Khieu nai lech so luong",
                "Khach hang " + userService.fullName(currentUser) + " khieu nai lech so luong don #" + orderId + ".",
                orderId,
                null,
                "/topic/admin-notifications"
        );

        return new ReceiptConfirmResponse(
                false,
                false,
                "Da gui khieu nai lech so luong den manager.",
                mismatches
        );
    }

    private User validateReceiptOwner(Order order) {
        User currentUser = currentUserService.requireCurrentUser();
        if (order.getUser() == null || !order.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.ORDER_CONFIRMATION_FORBIDDEN);
        }
        return currentUser;
    }

    private Task findCustomerReceiptTask(Long orderId) {
        Task task = queryWorkflowTask(orderId, "customer_confirm_receipt");
        if (task == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_NOT_AWAITING_RECEIPT_CONFIRMATION);
        }
        return task;
    }

    private Map<Long, Integer> buildReceivedQuantityMap(Order order, List<ItemCheckRequest> receivedItems) {
        if (receivedItems == null || receivedItems.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.RECEIVED_QUANTITY_LIST_REQUIRED);
        }
        Map<Long, OrderItem> orderItemsByVariant = buildOrderItemsByVariant(order);
        Map<Long, Integer> receivedByVariant = new HashMap<>();

        for (ItemCheckRequest requestItem : receivedItems) {
            if (requestItem == null || requestItem.getVariantId() == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.RECEIPT_VARIANT_ID_REQUIRED);
            }
            if (requestItem.getQuantity() < 0) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.RECEIVED_QUANTITY_NEGATIVE);
            }
            if (!orderItemsByVariant.containsKey(requestItem.getVariantId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.RECEIPT_VARIANT_NOT_IN_ORDER, requestItem.getVariantId());
            }
            if (receivedByVariant.put(requestItem.getVariantId(), requestItem.getQuantity()) != null) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.DUPLICATE_RECEIPT_VARIANT, requestItem.getVariantId());
            }
        }

        for (Long variantId : orderItemsByVariant.keySet()) {
            if (!receivedByVariant.containsKey(variantId)) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.RECEIVED_QUANTITY_MISSING, variantId);
            }
        }
        return receivedByVariant;
    }

    private Map<Long, OrderItem> buildOrderItemsByVariant(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_ITEM_REQUIRED_FOR_CONFIRMATION);
        }

        Map<Long, OrderItem> orderItemsByVariant = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            Long variantId = getOrderItemVariantId(item);
            if (orderItemsByVariant.put(variantId, item) != null) {
                throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.DUPLICATE_ORDER_VARIANT, variantId);
            }
        }
        return orderItemsByVariant;
    }

    private List<ReceiptMismatchDTO> buildReceiptMismatches(Order order, Map<Long, Integer> receivedByVariant) {
        List<ReceiptMismatchDTO> mismatches = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Long variantId = getOrderItemVariantId(item);
            int exportedQuantity = getExpectedReceiptQuantity(item);
            int receivedQuantity = receivedByVariant.get(variantId);
            if (receivedQuantity != exportedQuantity) {
                mismatches.add(new ReceiptMismatchDTO(
                        variantId,
                        getVariantName(item),
                        item.getQuantity(),
                        exportedQuantity,
                        receivedQuantity
                ));
            }
        }
        return mismatches;
    }

    private void applyReceivedQuantities(Order order, Map<Long, Integer> receivedByVariant) {
        for (OrderItem item : order.getItems()) {
            item.setReceivedQuantity(receivedByVariant.get(getOrderItemVariantId(item)));
        }
    }

    private int getExpectedReceiptQuantity(OrderItem item) {
        return item.getExportedQuantity() != null ? item.getExportedQuantity() : item.getQuantity();
    }

    private Long getOrderItemVariantId(OrderItem item) {
        if (item == null || item.getProductVariant() == null || item.getProductVariant().getId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_ITEM_VARIANT_INVALID);
        }
        return item.getProductVariant().getId();
    }

    private String getVariantName(OrderItem item) {
        ProductVariant variant = item.getProductVariant();
        if (variant == null || variant.getVariantName() == null || variant.getVariantName().isBlank()) {
            return "Variant #" + getOrderItemVariantId(item);
        }
        return variant.getVariantName();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReorderResponseDTO reorderOrder(Long orderId) {
        Order order = orderLookupService.require(orderId);
        String ownerId = order.getUser() == null ? null : order.getUser().getId();
        if (!currentUserService.isCurrentUserOwner(ownerId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Only delivered or cancelled orders can be reordered."
            );
        }

        List<ReorderItemDTO> addedItems = new ArrayList<>();
        List<ReorderItemDTO> skippedItems = new ArrayList<>();
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return new ReorderResponseDTO(0, 0, addedItems, skippedItems);
        }

        for (OrderItem item : order.getItems()) {
            Long variantId = item == null || item.getProductVariant() == null
                    ? null
                    : item.getProductVariant().getId();
            String variantName = buildReorderVariantName(item, variantId);
            int quantity = item == null ? 0 : Math.max(item.getQuantity(), 1);

            try {
                if (variantId == null) {
                    throw new AppException(
                            HttpStatus.BAD_REQUEST,
                            ConstantErrorCode.BAD_REQUEST_DETAIL,
                            "Order item does not have a valid product variant."
                    );
                }
                validateReorderItemAvailable(item, variantId);
                cartService.addToCart(CartService.CartOwner.user(ownerId), variantId, quantity);
                addedItems.add(new ReorderItemDTO(variantId, variantName, quantity, null));
            } catch (AppException e) {
                skippedItems.add(new ReorderItemDTO(variantId, variantName, quantity, e.getMessage()));
            } catch (RuntimeException e) {
                String skipReason = e.getMessage() == null ? "Cannot add this item to cart." : e.getMessage();
                skippedItems.add(new ReorderItemDTO(variantId, variantName, quantity, skipReason));
            }
        }

        return new ReorderResponseDTO(addedItems.size(), skippedItems.size(), addedItems, skippedItems);
    }

    private void validateReorderItemAvailable(OrderItem item, Long variantId) {
        ProductVariant variant = item == null ? null : item.getProductVariant();
        if (variant == null || variant.isDelete() || variant.getProduct() == null || variant.getProduct().isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND);
        }
        if (variant.getProduct().getAvailabilityStatus() != ProductAvailabilityStatus.ACCEPTING_ORDERS) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.PRODUCT_NOT_ACCEPTING_ORDERS,
                    variant.getProduct().getId()
            );
        }
    }

    private String buildReorderVariantName(OrderItem item, Long variantId) {
        if (item != null
                && item.getProductVariant() != null
                && item.getProductVariant().getVariantName() != null
                && !item.getProductVariant().getVariantName().isBlank()) {
            return item.getProductVariant().getVariantName();
        }
        return variantId == null ? "Unknown variant" : "Variant #" + variantId;
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderLookupService.require(id);
        assertCurrentUserCanViewOrder(order);
        return orderDetailsAssembler.toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getOrderHistory(Long orderId) {
        Order order = orderLookupService.require(orderId);
        assertCurrentUserCanViewOrder(order);
        return historyRepository.findByOrderIdOrderByUpdatetimeAsc(orderId)
                .stream().map(historyMapper::toDto).collect(Collectors.toList());
    }

    private void saveAndSendNotification(String title, String content, Long orderId, String targetUserId, String destination) {
        notificationService.sendNotification(title, content, orderId, targetUserId, null, destination);
    }

    private void saveCustomerNotificationIfSystemUser(Order order, String title, String content) {
        if (order == null || order.getUser() == null || order.getUser().getId() == null) {
            return;
        }
        saveAndSendNotification(
                title,
                content,
                order.getId(),
                order.getUser().getId(),
                "/topic/user-notifications/" + order.getUser().getId()
        );
    }

    @Transactional
    public void processMomoCallbackResult(Long orderId, String resultCode) {
        Order order = orderLookupService.requireForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING_ASSIGNMENT) {
            System.out.println("MoMo callback skipped because order #" + orderId + " was already processed.");
            return;
        }

        if ("0".equals(resultCode)) {
            handleSuccessfulMomoPayment(order);
        } else {
            handleFailedMomoPayment(order, resultCode);
        }

    }

    private void handleSuccessfulMomoPayment(Order order) {
        Long orderId = order.getId();
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        saveOrderAndAuditStatusChange(order, oldStatus, null);

        correlatePaymentSuccess(orderId);

        eventPublisher.publishAfterCommit(EventTypes.PAYMENT_CONFIRMED, new PaymentConfirmedEvent(orderId));
    }

    private void handleFailedMomoPayment(Order order, String resultCode) {
        Long orderId = order.getId();
        String cancelReason = "Thanh toan MoMo that bai hoac khach huy giao dich (Ma loi MoMo: " + resultCode + ")";
        orderCancellationService.cancelBySystem(
                orderId,
                order.getVersion(),
                CancellationSource.PAYMENT_FAILED,
                cancelReason,
                "momo-result:" + orderId + ":" + resultCode,
                "momo-result:" + orderId + ":" + resultCode
        );

    }

    private void correlatePaymentSuccess(Long orderId) {
        try {
            runtimeService.createMessageCorrelation("Msg_PaymentSuccess")
                    .processInstanceVariableEquals("orderId", orderId)
                    .correlate();
            System.out.println(">>> Camunda: Received MoMo payment for order #" + orderId + ".");
        } catch (Exception e) {
            System.err.println("Could not correlate MoMo payment success: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USER_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).userOrders(@currentUserService.requireCurrentUserId(), #minPrice, #maxPrice, #pageable)"
    )
    public Page<OrderListDTO> getMyOrders(Double minPrice, Double maxPrice, Pageable pageable) {
        String userId = currentUserService.requireCurrentUserId();
        return orderRepository.findListDtoByUserId(
                userId,
                List.of(
                        OrderStatus.PENDING_APPROVAL,
                        OrderStatus.PENDING_ASSIGNMENT,
                        OrderStatus.DISCUSSING,
                        OrderStatus.WAITING_STAFF_CONFIRMATION,
                        OrderStatus.ORDER_ACCEPTED,
                        OrderStatus.ORDER_CREATING,
                        OrderStatus.READY_TO_SHIP
                ),
                OrderStatus.SHIPPING,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                minPrice,
                maxPrice,
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USER_CANCELLED_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).userCancelledOrders(@currentUserService.requireCurrentUserId(), #minPrice, #maxPrice, #pageable)"
    )
    public Page<OrderListDTO> getMyCancelledOrders(Double minPrice, Double maxPrice, Pageable pageable) {
        String userId = currentUserService.requireCurrentUserId();
        return orderRepository.findListDtoByUserIdAndStatus(
                userId,
                OrderStatus.CANCELLED,
                OrderStatus.DELIVERED,
                minPrice,
                maxPrice,
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.STAFF_ASSIGNED_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).staffAssignedOrders(@currentUserService.requireCurrentUserId(), #pageable)"
    )
    public Page<OrderListDTO> getMyAssignedStaffOrders(Pageable pageable) {
        User staff = currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
        return orderRepository.findListDtoByWarehouseStaffIdAndStatusIn(
                staff.getId(),
                List.of(
                        OrderStatus.DISCUSSING,
                        OrderStatus.WAITING_STAFF_CONFIRMATION,
                        OrderStatus.ORDER_ACCEPTED,
                        OrderStatus.ORDER_CREATING,
                        OrderStatus.READY_TO_SHIP,
                        OrderStatus.SHIPPING,
                        OrderStatus.DELIVERED,
                        OrderStatus.CANCELLED
                ),
                List.of(
                        OrderStatus.DISCUSSING,
                        OrderStatus.WAITING_STAFF_CONFIRMATION,
                        OrderStatus.ORDER_ACCEPTED,
                        OrderStatus.ORDER_CREATING
                ),
                OrderStatus.DISCUSSING,
                OrderStatus.ORDER_CREATING,
                OrderStatus.READY_TO_SHIP,
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    public InventoryReservationService getInventoryReservationService() {
        return inventoryReservationService;
    }
}

