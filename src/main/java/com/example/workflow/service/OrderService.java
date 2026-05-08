package com.example.workflow.service;

import com.example.workflow.dto.AdminReviewRequest;
import com.example.workflow.dto.NotificationMessage;
import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
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

    @Transactional
    public void processMomoCallbackResult(Long orderId, String resultCode) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User user = order.getUser();

        if ("0".equals(resultCode)) {
            try {
                runtimeService.createMessageCorrelation("Msg_PaymentSuccess")
                        .processInstanceVariableEquals("orderId", orderId)
                        .correlate();
            } catch (Exception e) { }
            order.setStatus(OrderStatus.PENDING_WAREHOUSE);
            orderRepository.save(order);
            // SỬA THÀNH: Báo Admin kèm tên Khách
            saveAndSendNotification(
                    "Đơn hàng MoMo từ " + user.getLastname(),
                    "Khách hàng " + user.getLastname() + " đã thanh toán MoMo cho đơn #" + orderId + ". Vui lòng duyệt!",
                    orderId, null, "/topic/admin-notifications");

            // SỬA THÀNH: Báo User
            saveAndSendNotification(
                    "Thanh toán thành công! 💳",
                    "Đơn hàng #" + orderId + " của bạn đã được thanh toán và đang chờ Admin xét duyệt.",
                    orderId, user.getId(), "/topic/user-notifications/" + user.getId());
        }
    }

    @Transactional
    public void processAdminReview(Long orderId, AdminReviewRequest request, String adminName) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        Long customerId = order.getUser().getId();

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("admin_xac_nhan")
                .singleResult();

        if (task == null) throw new RuntimeException("Task không tồn tại");

        Map<String, Object> variables = new HashMap<>();
        if (request.isApproved()) {
            order.setStatus(OrderStatus.SHIPPING);
            variables.put("isApproved", true);
            // SỬA THÀNH: Báo User (Đơn được duyệt)
            saveAndSendNotification(
                    "Đơn hàng đang giao! 🚚",
                    "Đơn hàng #" + orderId + " của bạn đã được duyệt bởi Admin " + adminName + " và đang giao đến bạn.",
                    orderId, customerId, "/topic/user-notifications/" + customerId);
        } else {
            order.setCancelReason("Admin từ chối: " + request.getCancelReason());
            variables.put("isApproved", false);
            // SỬA THÀNH: Báo User (Đơn bị hủy)
            saveAndSendNotification(
                    "Đơn hàng bị hủy ❌",
                    "Đơn hàng #" + orderId + " của bạn đã bị từ chối bởi Admin " + adminName + ". Lý do: " + request.getCancelReason(),
                    orderId, customerId, "/topic/user-notifications/" + customerId);
            if (order.getUser().getEmail() != null && !order.getUser().getEmail().trim().isEmpty()) {
                new Thread(() -> {
                    emailService.sendOrderCancellationEmail(
                            order.getUser().getEmail(),
                            order.getUser().getLastname(),
                            orderId,
                            "Admin từ chối: " + request.getCancelReason()
                    );
                }).start();
            }
        }

        orderRepository.save(order);
        taskService.complete(task.getId(), variables);
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
    public void cancelOrder(Long id, String reason, String changer) {
        Order order = orderRepository.findById(id).orElseThrow();

        if (order.getStatus() != OrderStatus.PENDING_WAREHOUSE) {
            throw new IllegalStateException("Đơn hàng đã được xử lý, không thể tự hủy.");
        }

        User user = order.getUser();
        int deduction = (order.getTotalPrice() < 1000000) ? 1 : (order.getTotalPrice() <= 5000000) ? 2 : (order.getTotalPrice() <= 10000000) ? 3 : 5;
        if (user.getReputation() < deduction) {
            throw new IllegalStateException("Điểm uy tín của bạn không đủ để tự hủy đơn này.");
        }
        user.setReputation(user.getReputation() - deduction);
        userRepository.save(user);

        List<ProductVariant> variantsToUpdate = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getProductVariant();
                if (variant != null) {
                    variant.setQuantity(variant.getQuantity() + item.getQuantity());
                    variantsToUpdate.add(variant);
                }
            }
            productVariantRepository.saveAll(variantsToUpdate);
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

        saveAuditLog(order, OrderStatus.PENDING_WAREHOUSE, OrderStatus.CANCELLED, changer);
        clearRelatedCaches(user.getId(), variantsToUpdate);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("orderId", id)
                .singleResult();

        if (processInstance != null) {
            runtimeService.deleteProcessInstance(processInstance.getId(), "Khách hàng chủ động hủy đơn");
        }
        // SỬA THÀNH: Bắn 2 thông báo khi khách hàng tự hủy đơn
        saveAndSendNotification(
                "Khách hàng hủy đơn ⚠️",
                "Khách hàng " + user.getLastname() + " đã tự hủy đơn hàng #" + id + ". Lý do: " + reason,
                id, null, "/topic/admin-notifications");

        saveAndSendNotification(
                "Hủy đơn thành công",
                "Bạn đã hủy đơn hàng #" + id + " thành công.",
                id, user.getId(), "/topic/user-notifications/" + user.getId());
        saveAndSendNotification("Hủy đơn thành công", "Bạn đã hủy đơn hàng #" + id + " thành công.", id, user.getId(), "/topic/user-notifications/" + user.getId());

        // ==============================================================
        // 🚨 THÊM MỚI: BẮN EMAIL KHI KHÁCH TỰ HỦY
        // ==============================================================
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            new Thread(() -> {
                emailService.sendOrderCancellationEmail(
                        user.getEmail(),
                        user.getLastname(),
                        id,
                        "Khách hàng chủ động hủy: " + reason
                );
            }).start();
        }
    }

    // Các hàm phụ trợ giữ nguyên
    @Transactional
    public void confirmCustomerReceipt(Long orderId, String username) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.SHIPPING) throw new RuntimeException("Đơn hàng chưa được giao!");
        Task task = taskService.createTaskQuery().processVariableValueEquals("orderId", orderId).taskDefinitionKey("khach_nhan_h_ang").singleResult();
        if (task != null) taskService.complete(task.getId());
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);
        order.setEndOrderTime(LocalDateTime.now());
        User user = order.getUser();
        user.setReputation(user.getReputation() + 2);
        userRepository.save(user);
        orderRepository.save(order);
        saveAuditLog(order, oldStatus, OrderStatus.DELIVERED, username);
    }

    @Cacheable(value = "orders", key = "#user_id")
    public List<OrderDTO> getOrdersByUserId(Long user_id) {
        List<Order> orders = orderRepository.getOrdersByUserId(user_id);
        List<OrderDTO> dtos = orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
        for (int i = 0; i < orders.size(); i++) injectImageUrls(orders.get(i), dtos.get(i));
        return dtos;
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.getOrdersById(id);
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

    @Transactional
    public void updateStatus(Long id, String newStatusStr, String changer) {
        Order order = orderRepository.findById(id).orElseThrow();
        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        if (newStatus == OrderStatus.CANCELLED) throw new IllegalStateException("Vui lòng dùng API hủy đơn riêng biệt.");
        if (newStatus == OrderStatus.DELIVERED) {
            User user = order.getUser();
            user.setReputation(user.getReputation() + 2);
            order.setEndOrderTime(LocalDateTime.now());
            userRepository.save(user);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
        saveAuditLog(order, oldStatus, newStatus, changer);
        clearRelatedCaches(order.getUser().getId(), null);
    }

    private void saveAuditLog(Order order, OrderStatus oldStatus, OrderStatus newStatus, String changer) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldstatus(oldStatus);
        history.setNewstatus(newStatus);
        history.setUpdatetime(LocalDateTime.now());
        history.setChanger(changer);
        historyRepository.save(history);
    }

    private void clearRelatedCaches(Long userId, List<ProductVariant> updatedVariants) {
        Cache ordersCache = cacheManager.getCache("orders");
        if (ordersCache != null) ordersCache.evict(userId);
        Cache usersCache = cacheManager.getCache("users");
        if (usersCache != null) usersCache.clear();
        Cache userDetailCache = cacheManager.getCache("user");
        if (userDetailCache != null) userDetailCache.evict(userId);
        if (updatedVariants != null && !updatedVariants.isEmpty()) {
            Cache productDetailCache = cacheManager.getCache("product");
            Cache productsListCache = cacheManager.getCache("products");
            if (productDetailCache != null) updatedVariants.forEach(v -> productDetailCache.evict(v.getProduct().getId()));
            if (productsListCache != null) productsListCache.clear();
        }
    }
}