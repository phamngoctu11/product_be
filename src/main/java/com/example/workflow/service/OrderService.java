package com.example.workflow.service;

import com.example.workflow.dto.*;
import com.example.workflow.cache.DeferredCacheEvict;
import com.example.workflow.cache.DeferredCacheEvicts;
import com.example.workflow.entity.*;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancelledEvent;
import com.example.workflow.event.payload.OrderDeliveredEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.mapper.OrderStatusHistoryMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.*;
import com.example.workflow.service.redis.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final UserVoucherRepository userVoucherRepository;
    private final DomainEventPublisher eventPublisher;
    private final InventoryReservationService inventoryReservationService;
    private final AuthService authService;
    private final ReputationService reputationService;
    private final CartService cartService;
    private final ProductReviewRepository productReviewRepository;

    // Get current authenticated user.
    private User getCurrentAuthenticatedUser() {
        return authService.getCurrentUser();
    }

    private User getManagerReviewer(String changerId) {
        User manager = userRepository.findById(changerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.REVIEWER_NOT_FOUND, changerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.REVIEWER_MANAGER_ROLE_REQUIRED);
        }
        return manager;
    }

    private User getCurrentManager() {
        User manager = getCurrentAuthenticatedUser();
        if (manager.getRole() != Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CURRENT_USER_MANAGER_ROLE_REQUIRED);
        }
        return manager;
    }

    private User getCurrentStaff() {
        User staff = getCurrentAuthenticatedUser();
        if (staff.getRole() != Role.STAFF) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
        }
        return staff;
    }

    private User getStaffById(String staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.STAFF_NOT_FOUND, staffId));
        if (staff.getRole() != Role.STAFF || staff.isDelete()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ACTIVE_STAFF_REQUIRED);
        }
        return staff;
    }

    private String buildFullName(User user) {
        String lastname = user.getLastname() == null ? "" : user.getLastname().trim();
        String firstname = user.getFirstname() == null ? "" : user.getFirstname().trim();
        return (lastname + " " + firstname).trim();
    }

    private Pageable normalizePageable(Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null ? 20 : pageable.getPageSize();
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
    }

    private Order getOrderForUpdateOrThrow(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.ORDER_NOT_FOUND));
    }

    private void assertCurrentUserCanViewOrder(Order order) {
        User currentUser = getCurrentAuthenticatedUser();
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
            saveAuditLog(order, oldStatus, order.getStatus(), changerId);
        }
    }

    private void restoreVoucher(UserVoucher appliedVoucher) {
        if (appliedVoucher == null) {
            return;
        }
        appliedVoucher.setUsed(false);
        appliedVoucher.setUsedDate(null);
        userVoucherRepository.save(appliedVoucher);
    }

    private void deleteOrderProcessIfExists(Long orderId, String reason) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("orderId", orderId)
                .singleResult();
        if (processInstance != null) {
            runtimeService.deleteProcessInstance(processInstance.getId(), reason);
        }
    }

    private OrderItem findOrderItemByVariant(Order order, Long variantId) {
        return order.getItems().stream()
                .filter(existingItem -> existingItem.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VARIANT_NOT_IN_ORDER, variantId));
    }

    private void sendOrderConfirmationEmailAsync(User user, Order order, String paymentMethodLabel) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return;
        }
        emailService.sendOrderConfirmationEmail(
                user.getEmail(),
                user.getLastname(),
                order.getId(),
                resolveFinalPrice(order),
                paymentMethodLabel
        );
    }

    @Transactional
    @DeferredCacheEvicts(reason = "warehouse order claimed", value = {
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true),
            @DeferredCacheEvict(cacheName = "orders", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void claimWarehouseOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        User staff = getCurrentStaff();

        if (order.getStatus() != OrderStatus.PENDING_WAREHOUSE) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_NOT_WAITING_FOR_WAREHOUSE_STAFF);
        }
        if (order.getWarehouseStaff() != null) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.ORDER_ALREADY_ASSIGNED);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setWarehouseStaff(staff);
        order.setStatus(OrderStatus.WAREHOUSE_ASSIGNED);
        saveOrderAndAuditStatusChange(order, oldStatus, staff.getId());
    }

    @Transactional
    @DeferredCacheEvicts(reason = "staff assigned to order", value = {
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true),
            @DeferredCacheEvict(cacheName = "staffOrders", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true)
    })
    public void assignStaffToOrder(Long orderId, String staffId) {
        Order order = getOrderOrThrow(orderId);
        User manager = getCurrentManager();
        User staff = getStaffById(staffId);

        if (order.getStatus() != OrderStatus.PENDING_WAREHOUSE && order.getStatus() != OrderStatus.WAREHOUSE_ASSIGNED) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_CANNOT_BE_ASSIGNED);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setWarehouseStaff(staff);
        order.setStatus(OrderStatus.WAREHOUSE_ASSIGNED);
        saveOrderAndAuditStatusChange(order, oldStatus, manager.getId());
        saveAndSendNotification("Don hang moi duoc gan", "Don #" + orderId + " da duoc manager giao cho ban phu trach xuat kho.", orderId, staff.getId(), "/topic/user-notifications/" + staff.getId());
    }

    // ==========================================
    // Station 1: manager review
    // ==========================================
    @Transactional
    @DeferredCacheEvicts(reason = "manager reviewed order", value = {
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void processAdminReview(Long orderId, AdminReviewRequest request, String changerId, String staffId) {
        Order order = getOrderOrThrow(orderId);
        User manager = getManagerReviewer(changerId);
        User assignedStaff = request.isApproved() && staffId != null ? getStaffById(staffId) : null;

        Task task = findWorkflowTask(orderId, "manager_approve_order", "Order is not waiting for manager approval!");

        OrderStatus oldStatus = order.getStatus();
        order.setManager(manager);
        order.setApprovedById(manager.getId());
        order.setApprovedByFullName(buildFullName(manager));

        Map<String, Object> variables = new HashMap<>();
        if (request.isApproved()) {
            order.setWarehouseStaff(assignedStaff);
            order.setStatus(assignedStaff == null ? OrderStatus.PENDING_WAREHOUSE : OrderStatus.WAREHOUSE_ASSIGNED);
            variables.put("isApproved", true);
            if (assignedStaff != null) {
                saveAndSendNotification("Don hang moi duoc gan", "Don #" + orderId + " da duoc giao cho ban phu trach xuat kho.", orderId, assignedStaff.getId(), "/topic/user-notifications/" + assignedStaff.getId());
            }
            saveCustomerNotificationIfSystemUser(order, "Don hang da duyet", "Don #" + orderId + " dang duoc chuan bi.");
        } else {
            order.setWarehouseStaff(null);
            order.setCancelReason("Quan ly tu choi: " + request.getCancelReason());
            variables.put("isApproved", false);
            // Rejected orders are cancelled by the workflow delegate.
            saveCustomerNotificationIfSystemUser(order, "Don hang bi tu choi", "Ly do: " + request.getCancelReason());
            sendGuestCancellationEmailIfNeeded(order);
        }

        if (request.isApproved()) {
            saveOrderAndAuditStatusChange(order, oldStatus, manager.getId());
        } else {
            saveOrderAndAuditStatusChange(order, oldStatus, null);
        }
        taskService.complete(task.getId(), variables);
    }

    // ==========================================
    // Station 2: warehouse export
    // ==========================================
    @Transactional
    @DeferredCacheEvicts(reason = "staff exported order", value = {
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void processStaffExport(Long orderId, List<ItemCheckRequest> exportData) {
        Order order = getOrderOrThrow(orderId);
        User staff = getCurrentStaff();

        if (order.getStatus() != OrderStatus.WAREHOUSE_ASSIGNED) {
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
            findOrderItemByVariant(order, req.getVariantId()).setExportedQuantity(req.getQuantity());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.PENDING_KCS);
        saveOrderAndAuditStatusChange(order, oldStatus, staff.getId());
        taskService.complete(task.getId());
    }

    // ==========================================
    // Station 3: manager KCS reconciliation
    // ==========================================
    @Transactional
    @DeferredCacheEvicts(reason = "manager KCS checked order", value = {
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true),
            @DeferredCacheEvict(cacheName = "products", allEntries = true),
            @DeferredCacheEvict(cacheName = "product", allEntries = true),
            @DeferredCacheEvict(cacheName = "wishlistProducts", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void processManagerKcsCheck(Long orderId, boolean isPassed,String cancelReason) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.PENDING_KCS) {
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
            order.setStatus(OrderStatus.WAREHOUSE_ASSIGNED);
            String message = " vui long kiem tra lai so luong xuat.";
            if (cancelReason != null) message = "Ly do: " + cancelReason;
            String staffId = order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
            String destination = staffId == null ? "/topic/admin-notifications" : "/topic/user-notifications/" + staffId;
            saveAndSendNotification("Canh bao KCS", "Don # bi KCS danh rot" + orderId + message, orderId, staffId, destination);
        }

        saveOrderAndAuditStatusChange(order, oldStatus, null);
        taskService.complete(task.getId(), variables);
    }

    // ==========================================
    // Station 4: customer receipt confirmation
    // ==========================================
    @Transactional
    @DeferredCacheEvicts(reason = "customer confirmed receipt", value = {
            @DeferredCacheEvict(cacheName = "staffOrders", allEntries = true),
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true),
            @DeferredCacheEvict(cacheName = "bestSellingProducts", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", allEntries = true)
    })
    public ReceiptConfirmResponse confirmCustomerReceipt(Long orderId, ReceiptConfirmRequest request) {
        Order order = getOrderOrThrow(orderId);
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
        saveAndSendNotification(
                "Danh gia san pham",
                "Don hang #" + orderId + " da hoan tat. Hay chia se trai nghiem cua ban cho tung san pham.",
                orderId,
                currentUser.getId(),
                "/topic/user-notifications/" + currentUser.getId()
        );

        taskService.complete(task.getId());

        boolean matched = mismatches.isEmpty();
        String message = matched
                ? "Xac nhan nhan hang thanh cong. Cam on ban!"
                : "Xac nhan nhan hang thanh cong voi so luong thuc nhan bi lech da duoc chap nhan.";
        return new ReceiptConfirmResponse(matched, true, message, mismatches);
    }

    @Transactional
    public ReceiptConfirmResponse sendReceiptComplaint(Long orderId, ReceiptComplaintRequest request) {
        Order order = getOrderOrThrow(orderId);
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
                    buildFullName(currentUser),
                    currentUser.getEmail(),
                    request.getNote(),
                    mismatches
            );
        }

        saveAndSendNotification(
                "Khieu nai lech so luong",
                "Khach hang " + buildFullName(currentUser) + " khieu nai lech so luong don #" + orderId + ".",
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
        User currentUser = getCurrentAuthenticatedUser();
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

    private int calculateCancellationReputationDeduction(Order order) {
        double finalPrice = resolveFinalPrice(order);
        if (finalPrice < 1_000_000) {
            return 1;
        }
        if (finalPrice <= 5_000_000) {
            return 2;
        }
        if (finalPrice <= 10_000_000) {
            return 3;
        }
        return 5;
    }

    private double resolveFinalPrice(Order order) {
        if (order.getFinalPrice() != null) {
            return order.getFinalPrice();
        }
        double discountAmount = order.getDiscountAmount() == null ? 0.0 : order.getDiscountAmount();
        return Math.max(0.0, order.getTotalPrice() - discountAmount);
    }

    private void deductUserReputation(User user, int deduction, Long orderId) {
        reputationService.changeReputation(
                user,
                -deduction,
                "Cancelled order #" + orderId,
                "ORDER",
                String.valueOf(orderId)
        );
    }

    @Transactional
    @DeferredCacheEvicts(reason = "order cancelled", value = {
            @DeferredCacheEvict(cacheName = "warehouseOrders", allEntries = true),
            @DeferredCacheEvict(cacheName = "staffOrders", allEntries = true),
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true),
            @DeferredCacheEvict(cacheName = "products", allEntries = true),
            @DeferredCacheEvict(cacheName = "product", allEntries = true),
            @DeferredCacheEvict(cacheName = "wishlistProducts", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "userVoucherWallet", allEntries = true)
    })
    public void cancelOrder(Long id, String reason) {
        Order order = getOrderForUpdateOrThrow(id);
        User user = getCurrentAuthenticatedUser();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CANNOT_CANCEL_ANOTHER_USERS_ORDER);
        }

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        OrderStatus oldStatus = order.getStatus();
        deductUserReputation(user, calculateCancellationReputationDeduction(order), id);
        deleteOrderProcessIfExists(id, "Customer cancelled order");
        inventoryReservationService.releaseReservedStock(order, "CANCEL_RETURN");
        restoreVoucher(order.getUserVoucher());

        order.setCancelReason("Khach hang tu huy: " + reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setEndOrderTime(LocalDateTime.now());
        saveOrderAndAuditStatusChange(order, oldStatus, user.getId());
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLED,
                new OrderCancelledEvent(order.getId(), order.getCancelReason())
        );

        saveAndSendNotification("Khach hang huy don", "Don hang #" + id + " da bi huy.", id, null, "/topic/admin-notifications");
        saveAndSendNotification("Huy don thanh cong", "Ban da huy don hang #" + id + " thanh cong.", id, user.getId(), "/topic/user-notifications/" + user.getId());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReorderResponseDTO reorderOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        String ownerId = order.getUser() == null ? null : order.getUser().getId();
        if (!authService.isCurrentUserOwner(ownerId)) {
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
                validateReorderItemAvailable(item, variantId, quantity);
                cartService.startAddToCartProcess(ownerId, variantId, quantity);
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

    private void validateReorderItemAvailable(OrderItem item, Long variantId, int quantity) {
        ProductVariant variant = item == null ? null : item.getProductVariant();
        if (variant == null || variant.isDelete() || variant.getProduct() == null || variant.getProduct().isDelete()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VARIANT_NOT_FOUND);
        }
        if (variant.getQuantity() < quantity) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_VARIANT_OUT_OF_STOCK, variantId);
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
        Order order = getOrderOrThrow(id);
        assertCurrentUserCanViewOrder(order);
        OrderDTO dto = orderMapper.toDto(order);
        injectImageUrls(order, dto);
        injectReviewStatuses(dto);
        return dto;
    }

    private void injectImageUrls(Order entity, OrderDTO dto) {
        if (entity.getItems() != null && dto.getItems() != null) {
            for (int j = 0; j < entity.getItems().size(); j++) {
                OrderItem itemEntity = entity.getItems().get(j);
                var itemDTO = dto.getItems().get(j);
                if (itemEntity.getProductVariant() != null) {
                    ProductVariant variant = itemEntity.getProductVariant();
                    if (variant.getImageUrl() != null && !variant.getImageUrl().isEmpty()) itemDTO.setImageUrl(variant.getImageUrl());
                    else if (variant.getProduct() != null && variant.getProduct().getImageUrl() != null) itemDTO.setImageUrl(variant.getProduct().getImageUrl());
                }
            }
        }
    }

    private void injectReviewStatuses(OrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return;
        }

        List<Long> orderItemIds = dto.getItems().stream()
                .map(OrderItemDTO::getOrderItemId)
                .filter(id -> id != null)
                .toList();
        if (orderItemIds.isEmpty()) {
            return;
        }

        Map<Long, Long> reviewIdByOrderItemId = productReviewRepository.findByOrderItem_IdIn(orderItemIds)
                .stream()
                .collect(Collectors.toMap(review -> review.getOrderItem().getId(), ProductReview::getId));

        dto.getItems().forEach(item -> {
            Long reviewId = reviewIdByOrderItemId.get(item.getOrderItemId());
            item.setReviewed(reviewId != null);
            item.setReviewId(reviewId);
        });
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getOrderHistory(Long orderId) {
        Order order = getOrderOrThrow(orderId);
        assertCurrentUserCanViewOrder(order);
        return historyRepository.findByOrderIdOrderByUpdatetimeAsc(orderId)
                .stream().map(historyMapper::toDto).collect(Collectors.toList());
    }

    private void saveAuditLog(Order order, OrderStatus oldStatus, OrderStatus newStatus, String changer) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldstatus(oldStatus);
        history.setNewstatus(newStatus);
        history.setUpdatetime(LocalDateTime.now());
        history.setChangerId(changer);
        historyRepository.save(history);
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

    private void sendGuestCancellationEmailIfNeeded(Order order) {
        if (order == null || order.getUser() != null || order.getEmail() == null || order.getEmail().isBlank()) {
            return;
        }
        emailService.sendOrderCancellationEmail(
                order.getEmail(),
                order.getRecipientName(),
                order.getId(),
                order.getCancelReason()
        );
    }
    @Transactional
    @DeferredCacheEvicts(reason = "momo callback processed", value = {
            @DeferredCacheEvict(cacheName = "warehouseOrders", allEntries = true),
            @DeferredCacheEvict(cacheName = "staffOrders", allEntries = true),
            @DeferredCacheEvict(cacheName = "dashboardStats", allEntries = true)
    })
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "userVoucherWallet", allEntries = true)
    })
    public void processMomoCallbackResult(Long orderId, String resultCode) {
        Order order = getOrderForUpdateOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
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

        saveAndSendNotification(
                "Don Online moi da thanh toan",
                "Don hang #" + orderId + " da thanh toan qua MoMo va dang cho duyet.",
                orderId,
                null,
                "/topic/admin-notifications"
        );
        saveAndSendNotification(
                "Thanh toan thanh cong",
                "Ban da thanh toan thanh cong don hang #" + orderId + ". Cua hang dang chuan bi don.",
                orderId,
                order.getUser().getId(),
                "/topic/user-notifications/" + order.getUser().getId()
        );
        sendOrderConfirmationEmailAsync(order.getUser(), order, "Thanh toan Online qua MoMo");
    }

    private void handleFailedMomoPayment(Order order, String resultCode) {
        Long orderId = order.getId();
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Thanh toan MoMo that bai hoac khach huy giao dich (Ma loi MoMo: " + resultCode + ")");
        order.setEndOrderTime(LocalDateTime.now());

        inventoryReservationService.releaseReservedStock(order, "PAYMENT_FAILED_RETURN");
        restoreVoucher(order.getUserVoucher());
        saveOrderAndAuditStatusChange(order, oldStatus, null);
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLED,
                new OrderCancelledEvent(orderId, order.getCancelReason())
        );
        deleteOrderProcessIfExists(orderId, "MoMo payment failed");

        saveAndSendNotification(
                "Thanh toan that bai",
                "Giao dich cho don hang #" + orderId + " khong thanh cong. Don hang da bi huy.",
                orderId,
                order.getUser().getId(),
                "/topic/user-notifications/" + order.getUser().getId()
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
    @Cacheable(value = "orders", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #minPrice + '-' + #maxPrice + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getMyOrders(Double minPrice, Double maxPrice, Pageable pageable) {
        String userId = authService.getCurrentUserId();
        return orderRepository.findListDtoByUserId(
                userId,
                List.of(
                        OrderStatus.PENDING_PAYMENT,
                        OrderStatus.PENDING_APPROVAL,
                        OrderStatus.PENDING_WAREHOUSE,
                        OrderStatus.WAREHOUSE_ASSIGNED,
                        OrderStatus.PENDING_KCS
                ),
                OrderStatus.SHIPPING,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                minPrice,
                maxPrice,
                normalizePageable(pageable)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-cancelled-' + #minPrice + '-' + #maxPrice + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getMyCancelledOrders(Double minPrice, Double maxPrice, Pageable pageable) {
        String userId = authService.getCurrentUserId();
        return orderRepository.findListDtoByUserIdAndStatus(
                userId,
                OrderStatus.CANCELLED,
                OrderStatus.DELIVERED,
                minPrice,
                maxPrice,
                normalizePageable(pageable)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "warehouseOrders", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getWarehousePendingOrders(Pageable pageable) {
        return orderRepository.findUnassignedListDtoByStatus(OrderStatus.PENDING_WAREHOUSE, normalizePageable(pageable));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "staffOrders", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getMyAssignedStaffOrders(Pageable pageable) {
        User staff = getCurrentStaff();
        return orderRepository.findListDtoByWarehouseStaffIdAndStatusIn(
                staff.getId(),
                List.of(OrderStatus.WAREHOUSE_ASSIGNED, OrderStatus.PENDING_KCS, OrderStatus.SHIPPING),
                List.of(OrderStatus.WAREHOUSE_ASSIGNED, OrderStatus.PENDING_KCS),
                OrderStatus.WAREHOUSE_ASSIGNED,
                OrderStatus.PENDING_KCS,
                OrderStatus.SHIPPING,
                normalizePageable(pageable)
        );
    }

    // Get pending order list for manager.
    @Transactional(readOnly = true)
    @Cacheable(value = "pendingOrders", key = "#status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getPendingOrders(OrderStatus status,Pageable pageable) {
        // Read list DTOs directly from DB.
        return orderRepository.findListDtoByStatusOldestFirst(status, normalizePageable(pageable));
    }
}

