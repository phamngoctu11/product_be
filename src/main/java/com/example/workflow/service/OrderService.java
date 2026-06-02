package com.example.workflow.service;

import com.example.workflow.dto.*;
import com.example.workflow.entity.*;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.OrderItemMapper;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final OrderItemMapper orderItemMapper;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CacheManager cacheManager;
    private final OrderStatusHistoryMapper historyMapper;
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final UserVoucherRepository userVoucherRepository;

    // Lấy User đang đăng nhập an toàn tuyệt đối
    private User getCurrentAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndIsDeleteFalse(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng đăng nhập."));
    }

    private User getManagerReviewer(Long changerId) {
        User manager = userRepository.findById(changerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Reviewer not found with id: " + changerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, "Reviewer must have MANAGER role");
        }
        return manager;
    }

    private User getCurrentManager() {
        User manager = getCurrentAuthenticatedUser();
        if (manager.getRole() != Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, "Current user must have MANAGER role");
        }
        return manager;
    }

    private User getCurrentStaff() {
        User staff = getCurrentAuthenticatedUser();
        if (staff.getRole() != Role.STAFF) {
            throw new AppException(HttpStatus.FORBIDDEN, "Current user must have STAFF role");
        }
        return staff;
    }

    private User getStaffById(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));
        if (staff.getRole() != Role.STAFF || staff.isDelete()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Assigned user must be an active STAFF");
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

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void claimWarehouseOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User staff = getCurrentStaff();

        if (order.getStatus() != OrderStatus.PENDING_WAREHOUSE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Order is not waiting for warehouse staff");
        }
        if (order.getWarehouseStaff() != null) {
            throw new AppException(HttpStatus.CONFLICT, "Order has already been assigned to a staff member");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setWarehouseStaff(staff);
        order.setStatus(OrderStatus.WAREHOUSE_ASSIGNED);
        orderRepository.save(order);
        saveAuditLog(order, oldStatus, OrderStatus.WAREHOUSE_ASSIGNED, staff.getId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void assignStaffToOrder(Long orderId, Long staffId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User manager = getCurrentManager();
        User staff = getStaffById(staffId);

        if (order.getStatus() != OrderStatus.PENDING_WAREHOUSE && order.getStatus() != OrderStatus.WAREHOUSE_ASSIGNED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Order cannot be assigned at its current status");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setWarehouseStaff(staff);
        order.setStatus(OrderStatus.WAREHOUSE_ASSIGNED);
        orderRepository.save(order);

        if (oldStatus != OrderStatus.WAREHOUSE_ASSIGNED) {
            saveAuditLog(order, oldStatus, OrderStatus.WAREHOUSE_ASSIGNED, manager.getId());
        }
        saveAndSendNotification("Don hang moi duoc gan", "Don #" + orderId + " da duoc manager giao cho ban phu trach xuat kho.", orderId, staff.getId(), "/topic/user-notifications/" + staff.getId());
    }

    // ==========================================
    // TRẠM 1: QUẢN LÝ DUYỆT ĐƠN
    // ==========================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void processAdminReview(Long orderId, AdminReviewRequest request, Long changerId, Long staffId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User manager = getManagerReviewer(changerId);
        User assignedStaff = request.isApproved() && staffId != null ? getStaffById(staffId) : null;

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("manager_approve_order")
                .singleResult();
        if (task == null) throw new RuntimeException("Đơn hàng không ở trạng thái chờ duyệt!");

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
            saveAndSendNotification("Đơn hàng đã duyệt", "Đơn #" + orderId + " đang được chuẩn bị.", orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        } else {
            order.setWarehouseStaff(null);
            order.setCancelReason("Quản lý từ chối: " + request.getCancelReason());
            variables.put("isApproved", false);
            // Nếu từ chối, Delegate Cancel sẽ lo việc cập nhật status và hoàn kho
            saveAndSendNotification("Đơn hàng bị từ chối", "Lý do: " + request.getCancelReason(), orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        }

        orderRepository.save(order);
        if (request.isApproved()) {
            saveAuditLog(order, oldStatus, order.getStatus(), manager.getId());
        }
        taskService.complete(task.getId(), variables);
    }

    // ==========================================
    // TRẠM 2: NHÂN VIÊN XUẤT KHO
    // ==========================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void processStaffExport(Long orderId, List<ItemCheckRequest> exportData) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User staff = getCurrentStaff();

        if (order.getStatus() != OrderStatus.WAREHOUSE_ASSIGNED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Order must be assigned to staff before export");
        }
        if (order.getWarehouseStaff() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Order has no assigned warehouse staff");
        }
        if (!order.getWarehouseStaff().getId().equals(staff.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only assigned staff can export this order");
        }
        if (exportData == null || exportData.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Export data is required");
        }

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("staff_export_warehouse")
                .singleResult();
        if (task == null) throw new RuntimeException("Đơn hàng không ở trạng thái chờ xuất kho!");

        // Cập nhật số lượng thực xuất
        for (ItemCheckRequest req : exportData) {
            if (req.getQuantity() < 0) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Export quantity cannot be negative");
            }
            OrderItem orderItem = order.getItems().stream()
                    .filter(existingItem -> existingItem.getProductVariant().getId().equals(req.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Variant does not belong to this order: " + req.getVariantId()));
            orderItem.setExportedQuantity(req.getQuantity());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.PENDING_KCS);
        orderRepository.save(order);
        saveAuditLog(order, oldStatus, OrderStatus.PENDING_KCS, staff.getId());
        taskService.complete(task.getId());
    }

    // ==========================================
    // TRẠM 3: QUẢN LÝ KCS ĐỐI SOÁT
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
    public void processManagerKcsCheck(Long orderId, boolean isPassed) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (order.getStatus() != OrderStatus.PENDING_KCS) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Order is not waiting for KCS");
        }

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("manager_kcs_check")
                .singleResult();
        if (task == null) throw new RuntimeException("Không có Task KCS cho đơn hàng này!");

        Map<String, Object> variables = new HashMap<>();
        variables.put("kcsPassed", isPassed);

        OrderStatus oldStatus = order.getStatus();
        if (isPassed) {
            order.setStatus(OrderStatus.SHIPPING); // Bắt đầu giao hàng
            saveAndSendNotification("Đơn hàng đang giao 🚚", "Đơn hàng #" + orderId + " đã xuất kho và đang trên đường giao đến bạn.", orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        } else {
            order.setStatus(OrderStatus.WAREHOUSE_ASSIGNED);
            Long staffId = order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
            String destination = staffId == null ? "/topic/admin-notifications" : "/topic/user-notifications/" + staffId;
            saveAndSendNotification("Canh bao KCS", "Don #" + orderId + " bi KCS danh rot, vui long kiem tra lai so luong xuat.", orderId, staffId, destination);
        }

        orderRepository.save(order);
        saveAuditLog(order, oldStatus, order.getStatus(), null);
        taskService.complete(task.getId(), variables); // Nếu passed, Camunda sẽ tự gọi DeductInventoryDelegate ghi Sổ sao kê
    }

    // ==========================================
    // TRẠM 4: KHÁCH HÀNG NHẬN ĐƠN
    // ==========================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true),
            @CacheEvict(value = "bestSellingProducts", allEntries = true)
    })
    public void confirmCustomerReceipt(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User currentUser = getCurrentAuthenticatedUser();

        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng của người khác!");
        }

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("customer_confirm_receipt")
                .singleResult();
        if (task == null) throw new RuntimeException("Đơn hàng chưa được giao đến bạn!");

        // Cập nhật số lượng thực nhận
        for (ItemCheckRequest req : orderItemMapper.toCheckRequest(order.getItems())) {
            order.getItems().stream()
                    .filter(item -> item.getProductVariant().getId().equals(req.getVariantId()))
                    .findFirst()
                    .ifPresent(item -> item.setReceivedQuantity(req.getQuantity()));
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);
        order.setEndOrderTime(LocalDateTime.now());

        currentUser.setReputation(currentUser.getReputation() + 2);
        userRepository.save(currentUser);
        orderRepository.save(order);

        saveAuditLog(order, oldStatus, OrderStatus.DELIVERED, currentUser.getId());

        taskService.complete(task.getId());
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
        Order order = orderRepository.findById(id).orElseThrow();
        User user = getCurrentAuthenticatedUser();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You cannot cancel another user's order");
        }

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Order was approved or processed and cannot be cancelled by user");
        }

        OrderStatus oldStatus = order.getStatus();

        int deduction = (order.getTotalPrice() < 1000000) ? 1 : (order.getTotalPrice() <= 5000000) ? 2 : (order.getTotalPrice() <= 10000000) ? 3 : 5;
        if (user.getReputation() < deduction) {
            throw new IllegalStateException("Điểm uy tín của bạn không đủ để tự hủy đơn này.");
        }
        user.setReputation(user.getReputation() - deduction);
        userRepository.save(user);

        // Hủy Process Camunda
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("orderId", id)
                .singleResult();
        if (processInstance != null) {
            runtimeService.deleteProcessInstance(processInstance.getId(), "Khách hàng chủ động hủy đơn");
        }

        // Tự động xử lý như Delegate
        List<ProductVariant> variantsToUpdate = new ArrayList<>();
        if (order.getItems() != null && order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            // (Chỉ hoàn kho nếu Camunda đã lỡ trừ, nhưng logic mới thì KCS mới trừ, nên chỗ này an toàn)
        }

        UserVoucher appliedVoucher = order.getUserVoucher();
        if (appliedVoucher != null) {
            appliedVoucher.setUsed(false);
            appliedVoucher.setUsedDate(null);
        }

        order.setCancelReason("Khách hàng tự hủy: " + reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setEndOrderTime(LocalDateTime.now());
        orderRepository.save(order);

        saveAuditLog(order, oldStatus, OrderStatus.CANCELLED, user.getId());
        clearRelatedCaches(user.getId(), variantsToUpdate);

        saveAndSendNotification("Khách hàng hủy đơn ⚠️", "Đơn hàng #" + id + " đã bị hủy.", id, null, "/topic/admin-notifications");
        saveAndSendNotification("Hủy đơn thành công", "Bạn đã hủy đơn hàng #" + id + " thành công.", id, user.getId(), "/topic/user-notifications/" + user.getId());
    }

    // (Giữ nguyên các hàm phụ trợ lấy danh sách Order và Cache của bạn ở dưới đây)


    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
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

    private void saveAuditLog(Order order, OrderStatus oldStatus, OrderStatus newStatus, Long changer) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldstatus(oldStatus);
        history.setNewstatus(newStatus);
        history.setUpdatetime(LocalDateTime.now());
        history.setChangerId(changer);
        historyRepository.save(history);
    }

    private void clearRelatedCaches(Long userId, List<ProductVariant> updatedVariants) {
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

    private void saveAndSendNotification(String title, String content, Long orderId, Long targetUserId, String destination) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setOrderId(orderId);
        notification.setTargetUserId(targetUserId);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend(destination, notification);
    }
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "pendingOrders", allEntries = true),
            @CacheEvict(value = "warehouseOrders", allEntries = true),
            @CacheEvict(value = "staffOrders", allEntries = true)
    })
    public void processMomoCallbackResult(Long orderId, String resultCode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 🚨 CẢI TIẾN 1: BẢO VỆ IDEMPOTENCY (Chống Webhook gọi 2 lần)
        // Chỉ xử lý nếu đơn hàng đang ở đúng trạng thái chờ thanh toán
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            System.out.println("⚠️ Webhook MoMo: Đơn hàng #" + orderId + " đã được xử lý trước đó. Bỏ qua.");
            return;
        }

        if ("0".equals(resultCode)) {
            // ==========================================
            // KỊCH BẢN 1: THANH TOÁN THÀNH CÔNG
            // ==========================================
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(OrderStatus.PENDING_APPROVAL); // 🚨 CẢI TIẾN 2: Chuyển thẳng về cho Manager duyệt (Trạm 1)
            orderRepository.save(order);

            // Ghi sổ Audit Log (Truyền changerId = null vì đây là Hệ thống tự động làm)
            saveAuditLog(order, oldStatus, OrderStatus.PENDING_APPROVAL, null);

            // 🚨 CẢI TIẾN 3: Đánh thức luồng Camunda đang ngủ chờ thanh toán đi tiếp
            try {
                runtimeService.createMessageCorrelation("Msg_PaymentSuccess")
                        .processInstanceVariableEquals("orderId", orderId)
                        .correlate();
                System.out.println(">>> Camunda: Đã nhận thanh toán MoMo, chuyển đơn #" + orderId + " sang Trạm Duyệt Đơn.");
            } catch (Exception e) {
                System.err.println("Lỗi khi đánh thức Camunda: " + e.getMessage());
            }

            // Bắn thông báo Real-time
            saveAndSendNotification("Đơn Online mới (Đã thanh toán) 💰",
                    "Đơn hàng #" + orderId + " đã thanh toán qua MoMo, đang chờ bạn duyệt.",
                    orderId, null, "/topic/admin-notifications");

            saveAndSendNotification("Thanh toán thành công 🎉",
                    "Bạn đã thanh toán thành công đơn hàng #" + orderId + ". Cửa hàng đang chuẩn bị đơn.",
                    orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());

            // Gửi Hóa Đơn Email Tự Động (Chạy nền)
            if (order.getUser().getEmail() != null && !order.getUser().getEmail().trim().isEmpty()) {
                new Thread(() -> {
                    emailService.sendOrderConfirmationEmail(
                            order.getUser().getEmail(),
                            order.getUser().getLastname(),
                            orderId,
                            order.getTotalPrice(),
                            "Thanh toán Online qua MoMo"
                    );
                }).start();
            }

        } else {
            // ==========================================
            // KỊCH BẢN 2: THANH TOÁN THẤT BẠI HOẶC HỦY GIAO DỊCH
            // ==========================================
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelReason("Thanh toán MoMo thất bại hoặc khách chủ động hủy (Mã lỗi MoMo: " + resultCode + ")");
            order.setEndOrderTime(LocalDateTime.now());

            // Hoàn trả Voucher lại cho khách (nếu có dùng)
            UserVoucher appliedVoucher = order.getUserVoucher();
            if (appliedVoucher != null) {
                appliedVoucher.setUsed(false);
                appliedVoucher.setUsedDate(null);
                userVoucherRepository.save(appliedVoucher);
            }
            orderRepository.save(order);

            // Ghi log
            saveAuditLog(order, oldStatus, OrderStatus.CANCELLED, null);

            // 🚨 CẢI TIẾN 4: Hủy luôn Process Camunda để dọn dẹp bộ nhớ
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .variableValueEquals("orderId", orderId)
                    .singleResult();
            if (processInstance != null) {
                runtimeService.deleteProcessInstance(processInstance.getId(), "Thanh toán MoMo thất bại");
            }

            // Báo cho khách biết
            saveAndSendNotification("Thanh toán thất bại ❌",
                    "Giao dịch cho đơn hàng #" + orderId + " không thành công. Đơn hàng đã bị hủy.",
                    orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        }

        clearRelatedCaches(order.getUser().getId(), null);
    }
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#user_id + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderDTO> getOrdersByUserId(Long user_id, Pageable pageable) {
        Page<Long> orderIdPage = orderRepository.findOrderIdsByUserId(
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
                normalizePageable(pageable)
        );

        if (orderIdPage.isEmpty()) {
            return orderIdPage.map(orderId -> (OrderDTO) null);
        }

        List<Order> orders = orderRepository.findFullOrdersByIds(orderIdPage.getContent());
        Map<Long, Order> ordersById = orders.stream()
                .collect(Collectors.toMap(Order::getId, order -> order));

        return orderIdPage.map(orderId -> {
            Order order = ordersById.get(orderId);
            if (order == null) {
                throw new AppException(HttpStatus.NOT_FOUND, "Order not found with id: " + orderId);
            }
            OrderDTO dto = orderMapper.toDto(order);
            injectImageUrls(order, dto);
            return dto;
        });
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

    // Hàm lấy danh sách chờ duyệt cho Manager
    @Transactional(readOnly = true)
    @Cacheable(value = "pendingOrders", key = "#status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<OrderListDTO> getPendingOrders(OrderStatus status,Pageable pageable) {
        // Lấy thẳng List DTO từ DB
        return orderRepository.findListDtoByStatusOldestFirst(status, normalizePageable(pageable));
    }
}
