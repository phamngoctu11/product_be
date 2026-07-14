package com.example.workflow.service;

import com.example.workflow.dto.*;
import com.example.workflow.entity.*;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.mapper.OrderStatusHistoryMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CacheManager cacheManager;
    private final OrderStatusHistoryMapper historyMapper;
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserVoucherRepository userVoucherRepository;
    private final ConsultationAttributionService consultationAttributionService;
    private final InventoryReservationService inventoryReservationService;

    // Get current authenticated user.
    private User getCurrentAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
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
        new Thread(() -> emailService.sendOrderConfirmationEmail(
                user.getEmail(),
                user.getLastname(),
                order.getId(),
                order.getTotalPrice(),
                paymentMethodLabel
        )).start();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
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
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
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
            saveAndSendNotification("Don hang da duyet", "Don #" + orderId + " dang duoc chuan bi.", orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        } else {
            order.setWarehouseStaff(null);
            order.setCancelReason("Quan ly tu choi: " + request.getCancelReason());
            variables.put("isApproved", false);
            // Rejected orders are cancelled by the workflow delegate.
            saveAndSendNotification("Don hang bi tu choi", "Ly do: " + request.getCancelReason(), orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
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
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
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
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true)
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
            saveAndSendNotification("Don hang dang giao", "Don hang #" + orderId + " da xuat kho va dang tren duong giao den ban.", orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
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
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true)
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

        currentUser.setReputation(currentUser.getReputation() + 2);
        userRepository.save(currentUser);
        saveOrderAndAuditStatusChange(order, oldStatus, currentUser.getId());
        consultationAttributionService.confirmOrderAttributions(order.getId());

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
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.MANAGER_EMAIL_NOT_FOUND);
        }

        emailService.sendReceiptComplaintEmail(
                managerEmails,
                orderId,
                buildFullName(currentUser),
                currentUser.getEmail(),
                request.getNote(),
                mismatches
        );
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
        double totalPrice = order.getTotalPrice();
        if (totalPrice < 1_000_000) {
            return 1;
        }
        if (totalPrice <= 5_000_000) {
            return 2;
        }
        if (totalPrice <= 10_000_000) {
            return 3;
        }
        return 5;
    }

    private void deductUserReputation(User user, int deduction) {
        if (user.getReputation() < deduction) {
            throw new IllegalStateException("Diem uy tin cua ban khong du de tu huy don nay.");
        }
        user.setReputation(user.getReputation() - deduction);
        userRepository.save(user);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true)
    })
    public void cancelOrder(Long id, String reason) {
        Order order = getOrderOrThrow(id);
        User user = getCurrentAuthenticatedUser();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CANNOT_CANCEL_ANOTHER_USERS_ORDER);
        }

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        OrderStatus oldStatus = order.getStatus();
        deductUserReputation(user, calculateCancellationReputationDeduction(order));
        deleteOrderProcessIfExists(id, "Customer cancelled order");
        inventoryReservationService.releaseReservedStock(order, "CANCEL_RETURN");
        restoreVoucher(order.getUserVoucher());

        order.setCancelReason("Khach hang tu huy: " + reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setEndOrderTime(LocalDateTime.now());
        saveOrderAndAuditStatusChange(order, oldStatus, user.getId());
        consultationAttributionService.cancelOrderAttributions(order.getId());

        clearRelatedCaches(user.getId());

        saveAndSendNotification("Khach hang huy don", "Don hang #" + id + " da bi huy.", id, null, "/topic/admin-notifications");
        saveAndSendNotification("Huy don thanh cong", "Ban da huy don hang #" + id + " thanh cong.", id, user.getId(), "/topic/user-notifications/" + user.getId());
    }

    public OrderDTO getOrderById(Long id) {
        Order order = getOrderOrThrow(id);
        OrderDTO dto = orderMapper.toDto(order);
        injectImageUrls(order, dto);
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

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getOrderHistory(Long orderId) {
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

    private void clearRelatedCaches(String userId) {
        Cache ordersCache = cacheManager.getCache("orders");
        if (ordersCache != null) ordersCache.clear();
        Cache pendingOrdersCache = cacheManager.getCache("pendingOrders");
        if (pendingOrdersCache != null) pendingOrdersCache.clear();
        Cache warehouseOrdersCache = cacheManager.getCache("warehouseOrders");
        if (warehouseOrdersCache != null) warehouseOrdersCache.clear();
        Cache staffOrdersCache = cacheManager.getCache("staffOrders");
        if (staffOrdersCache != null) staffOrdersCache.clear();
        Cache usersCache = cacheManager.getCache("users");
        if (usersCache != null) usersCache.clear();
        Cache userDetailCache = cacheManager.getCache("user");
        if (userDetailCache != null) userDetailCache.evict(userId);
    }

    private void saveAndSendNotification(String title, String content, Long orderId, String targetUserId, String destination) {
        notificationService.sendNotification(title, content, orderId, targetUserId, null, destination);
    }
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void processMomoCallbackResult(Long orderId, String resultCode) {
        Order order = getOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            System.out.println("MoMo callback skipped because order #" + orderId + " was already processed.");
            return;
        }

        if ("0".equals(resultCode)) {
            handleSuccessfulMomoPayment(order);
        } else {
            handleFailedMomoPayment(order, resultCode);
        }

        clearRelatedCaches(order.getUser().getId());
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
        consultationAttributionService.cancelOrderAttributions(orderId);
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
    @Cacheable(value = "orders", key = "#user_id + '-' + #minPrice + '-' + #maxPrice + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getOrdersByUserId(String user_id, Double minPrice, Double maxPrice, Pageable pageable) {
        return orderRepository.findListDtoByUserId(
                user_id,
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
    @Cacheable(value = "orders", key = "'cancelled-' + #user_id + '-' + #minPrice + '-' + #maxPrice + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getCancelledOrdersByUserId(String user_id, Double minPrice, Double maxPrice, Pageable pageable) {
        return orderRepository.findListDtoByUserIdAndStatus(
                user_id,
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

