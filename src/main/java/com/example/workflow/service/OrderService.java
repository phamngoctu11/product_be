package com.example.workflow.service;

import com.example.workflow.dto.*;
import com.example.workflow.entity.*;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.mapper.OrderStatusHistoryMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
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
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
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

    // ==========================================
    // TRẠM 1: QUẢN LÝ DUYỆT ĐƠN
    // ==========================================
    @Transactional
    public void processAdminReview(Long orderId, AdminReviewRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User manager = getCurrentAuthenticatedUser(); // Lấy Chủ shop

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("manager_approve_order")
                .singleResult();
        if (task == null) throw new RuntimeException("Đơn hàng không ở trạng thái chờ duyệt!");

        order.setManager(manager); // Ghi nhận trách nhiệm Manager

        Map<String, Object> variables = new HashMap<>();
        if (request.isApproved()) {
            order.setStatus(OrderStatus.PENDING_WAREHOUSE); // Chuyển xuống kho
            variables.put("isApproved", true);
            saveAndSendNotification("Đơn hàng đã duyệt", "Đơn #" + orderId + " đang được chuẩn bị.", orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        } else {
            order.setCancelReason("Quản lý từ chối: " + request.getCancelReason());
            variables.put("isApproved", false);
            // Nếu từ chối, Delegate Cancel sẽ lo việc cập nhật status và hoàn kho
            saveAndSendNotification("Đơn hàng bị từ chối", "Lý do: " + request.getCancelReason(), orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        }

        orderRepository.save(order);
        taskService.complete(task.getId(), variables);
    }

    // ==========================================
    // TRẠM 2: NHÂN VIÊN XUẤT KHO
    // ==========================================
    @Transactional
    public void processStaffExport(Long orderId, List<ItemCheckRequest> exportData) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User staff = getCurrentAuthenticatedUser(); // Lấy nhân viên kho

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("staff_export_warehouse")
                .singleResult();
        if (task == null) throw new RuntimeException("Đơn hàng không ở trạng thái chờ xuất kho!");

        order.setWarehouseStaff(staff); // Ghi nhận trách nhiệm Staff

        // Cập nhật số lượng thực xuất
        for (ItemCheckRequest req : exportData) {
            order.getItems().stream()
                    .filter(item -> item.getProductVariant().getId().equals(req.getVariantId()))
                    .findFirst()
                    .ifPresent(item -> item.setExportedQuantity(req.getQuantity()));
        }

        orderRepository.save(order);
        taskService.complete(task.getId());
    }

    // ==========================================
    // TRẠM 3: QUẢN LÝ KCS ĐỐI SOÁT
    // ==========================================
    @Transactional
    public void processManagerKcsCheck(Long orderId, boolean isPassed) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("manager_kcs_check")
                .singleResult();
        if (task == null) throw new RuntimeException("Không có Task KCS cho đơn hàng này!");

        Map<String, Object> variables = new HashMap<>();
        variables.put("kcsPassed", isPassed);

        if (isPassed) {
            order.setStatus(OrderStatus.SHIPPING); // Bắt đầu giao hàng
            saveAndSendNotification("Đơn hàng đang giao 🚚", "Đơn hàng #" + orderId + " đã xuất kho và đang trên đường giao đến bạn.", orderId, order.getUser().getId(), "/topic/user-notifications/" + order.getUser().getId());
        } else {
            // Trả về cho nhân viên nhặt lại (Không đổi status)
            saveAndSendNotification("Cảnh báo KCS", "Đơn #" + orderId + " bị KCS đánh rớt, vui lòng kiểm tra lại số lượng xuất.", orderId, order.getWarehouseStaff().getId(), "/topic/admin-notifications");
        }

        orderRepository.save(order);
        taskService.complete(task.getId(), variables); // Nếu passed, Camunda sẽ tự gọi DeductInventoryDelegate ghi Sổ sao kê
    }

    // ==========================================
    // TRẠM 4: KHÁCH HÀNG NHẬN ĐƠN
    // ==========================================
    @Transactional
    public void confirmCustomerReceipt(Long orderId, List<ItemCheckRequest> receiptData) {
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
        for (ItemCheckRequest req : receiptData) {
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

        taskService.complete(task.getId()); // Sẽ gọi ReconciliationDelegate kiểm tra thất thoát
    }

    // ==========================================
    // KHÁCH HÀNG TỰ HỦY ĐƠN (Cập nhật Security)
    // ==========================================
    @Transactional
    public void cancelOrder(Long id, String reason) {
        Order order = orderRepository.findById(id).orElseThrow();
        User user = getCurrentAuthenticatedUser();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng của người khác!");
        }

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL && order.getStatus() != OrderStatus.PENDING_WAREHOUSE) {
            throw new IllegalStateException("Đơn hàng đã được xử lý sâu, không thể tự hủy.");
        }

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

        saveAuditLog(order, order.getStatus(), OrderStatus.CANCELLED, user.getId());
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
        if (ordersCache != null) ordersCache.evict(userId);
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
    @Cacheable(value = "orders", key = "#user_id")
    public List<OrderListDTO> getOrdersByUserId(Long user_id) {
        // Lấy thẳng List DTO từ Repository, tốc độ ánh sáng!
        return orderRepository.findListDtoByUserId(user_id);
    }

    // Hàm lấy danh sách chờ duyệt cho Manager
    @Transactional(readOnly = true)
    public List<OrderListDTO> getPendingOrders() {
        // Lấy thẳng List DTO từ DB
        return orderRepository.findListDtoByStatus(OrderStatus.PENDING_APPROVAL);
    }
}