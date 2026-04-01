package com.example.workflow.service;

import com.example.workflow.dto.AdminReviewRequest; // Nhớ tạo DTO này như bài trước
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

    // TIÊM CAMUNDA VÀO
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    // ==========================================
    // CÁC HÀM XỬ LÝ QUY TRÌNH (USER TASKS & IPN)
    // ==========================================

    @Transactional
    public void processMomoCallbackResult(Long orderId, String resultCode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if ("0".equals(resultCode)) {
            try {
                runtimeService.createMessageCorrelation("Msg_PaymentSuccess")
                        .processInstanceVariableEquals("orderId", orderId)
                        .correlate();
            } catch (Exception e) {
                System.out.println("Cảnh báo: Không tìm thấy luồng Camunda đang chờ cho Order " + orderId);
            }
            order.setStatus(OrderStatus.PENDING_WAREHOUSE);
            orderRepository.save(order);
        } else {
            System.out.println("Giao dịch MoMo thất bại/Hủy. Mã: " + resultCode);
        }
    }
    @Transactional
    public void processAdminReview(Long orderId, AdminReviewRequest request, String adminName) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("admin_xac_nhan")
                .singleResult();

        if (task == null) throw new RuntimeException("Task không tồn tại");

        Map<String, Object> variables = new HashMap<>();
        if (request.isApproved()) {
            order.setStatus(OrderStatus.SHIPPING);
            variables.put("isApproved", true);
        } else {
            order.setCancelReason("Admin từ chối: " + request.getCancelReason());
            variables.put("isApproved", false); // Sẽ chạy vào CancelOrderDelegate
        }

        orderRepository.save(order);
        taskService.complete(task.getId(), variables);
    }

    @Transactional
    public void confirmCustomerReceipt(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new RuntimeException("Đơn hàng chưa được giao, không thể xác nhận!");
        }

        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("orderId", orderId)
                .taskDefinitionKey("khach_nhan_h_ang")
                .singleResult();

        if (task != null) {
            taskService.complete(task.getId());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);
        order.setEndOrderTime(LocalDateTime.now());

        // Cộng uy tín
        User user = order.getUser();
        user.setReputation(user.getReputation() + 2);
        userRepository.save(user);

        orderRepository.save(order);
        saveAuditLog(order, oldStatus, OrderStatus.DELIVERED, username);
    }


    // ==========================================
    // CÁC HÀM GET & UPDATE CŨ GIỮ NGUYÊN
    // ==========================================

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
                    if (variant.getImageUrl() != null && !variant.getImageUrl().isEmpty()) {
                        itemDTO.setImageUrl(variant.getImageUrl());
                    } else if (variant.getProduct() != null && variant.getProduct().getImageUrl() != null) {
                        itemDTO.setImageUrl(variant.getProduct().getImageUrl());
                    }
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
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus;

        try { newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái mới không hợp lệ: " + newStatusStr); }

        if (newStatus == OrderStatus.CANCELLED) throw new IllegalStateException("Vui lòng dùng API hủy đơn riêng biệt để hủy đơn hàng.");

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

    @Transactional
    public void cancelOrder(Long id, String reason, String changer) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        // Điều kiện: Chỉ cho phép khách hủy khi đơn đang ở kho chờ xác nhận
        if (order.getStatus() != OrderStatus.PENDING_WAREHOUSE) {
            throw new IllegalStateException("Đơn hàng đã được xử lý, không thể tự hủy.");
        }

        User user = order.getUser();
        double totalPrice = order.getTotalPrice();

        // 1. Trừ điểm uy tín
        int deduction = (totalPrice < 1000000) ? 1 : (totalPrice <= 5000000) ? 2 : (totalPrice <= 10000000) ? 3 : 5;
        if (user.getReputation() < deduction) {
            throw new IllegalStateException("Điểm uy tín của bạn không đủ để tự hủy đơn này.");
        }
        user.setReputation(user.getReputation() - deduction);
        userRepository.save(user);

        // 2. Hoàn lại tồn kho
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

        // 3. Hoàn lại Voucher (nếu có)
        UserVoucher appliedVoucher = order.getUserVoucher();
        if (appliedVoucher != null) {
            appliedVoucher.setUsed(false);
            appliedVoucher.setUsedDate(null);
            // userVoucherRepository.save(appliedVoucher); // Nếu có repository thì bật lên
        }

        // 4. Lưu trạng thái CANCELLED trực tiếp vào DB
        order.setCancelReason("Khách hàng tự hủy: " + reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setEndOrderTime(LocalDateTime.now());
        orderRepository.save(order);

        // 5. Lưu Audit Log và Xóa Cache (để FE cập nhật danh sách)
        saveAuditLog(order, OrderStatus.PENDING_WAREHOUSE, OrderStatus.CANCELLED, changer);
        clearRelatedCaches(user.getId(), variantsToUpdate);

        // =================================================================
        // 🚨 TUYỆT KỸ: TÌM VÀ "GIẾT" LUỒNG CAMUNDA ĐANG CHẠY 🚨
        // =================================================================
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("orderId", id)
                .singleResult();

        if (processInstance != null) {
            // Lệnh này sẽ tiêu diệt luồng Camunda, xóa mọi Task liên quan khỏi hệ thống
            runtimeService.deleteProcessInstance(processInstance.getId(), "Khách hàng chủ động hủy đơn");
            System.out.println(">>> Camunda: Khách tự hủy -> Đã TIÊU DIỆT process instance của Order " + id);
        }
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