package com.example.workflow.service;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.OrderStatusHistory;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.User;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.mapper.OrderStatusHistoryMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderStatusHistoryRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderStatusHistoryRepository historyRepository; // Bổ sung repo
    private final CacheManager cacheManager;
    private final OrderStatusHistoryMapper historyMapper;

    @Cacheable(value = "orders", key = "#user_id")
    public List<OrderDTO> getOrdersByUserId(Long user_id) {
        return orderRepository.getOrdersByUserId(user_id).stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long id) {
        return orderMapper.toDto(orderRepository.getOrdersById(id));
    }
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getOrderHistory(Long orderId) {
        return historyRepository.findByOrderIdOrderByUpdatetimeAsc(orderId)
                .stream()
                .map(historyMapper::toDto)
                .collect(Collectors.toList());
    }

    // 1. CẬP NHẬT TRẠNG THÁI (Giữ nguyên tên cũ)
    @Transactional
    public void updateStatus(Long id, String newStatusStr, String changer) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus;

        try {
            newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái mới không hợp lệ: " + newStatusStr);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Vui lòng dùng API hủy đơn riêng biệt để hủy đơn hàng.");
        }

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

    // 2. HỦY ĐƠN HÀNG
    @Transactional
    public void cancelOrder(Long id, String reason, String changer) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        OrderStatus oldStatus = order.getStatus();

        if (oldStatus != OrderStatus.PENDING_WAREHOUSE) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi đang ở trạng thái Đang xuất kho.");
        }

        User user = order.getUser();
        double totalPrice = order.getTotalPrice();
        int deduction = (totalPrice < 1000000) ? 1 : (totalPrice <= 5000000) ? 2 : (totalPrice <= 10000000) ? 3 : 5;

        if (user.getReputation() < deduction) {
            throw new IllegalStateException("Điểm uy tín hiện tại của bạn không đủ để tự hủy đơn này.");
        }

        user.setReputation(user.getReputation() - deduction);
        List<Product> productsToUpdate = new ArrayList<>();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    product.setQuantity(product.getQuantity() + item.getQuantity());
                    productsToUpdate.add(product);
                }
            }
            productRepository.saveAll(productsToUpdate);
        }

        order.setCancelReason(reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setEndOrderTime(LocalDateTime.now());

        orderRepository.save(order);
        userRepository.save(user);

        saveAuditLog(order, oldStatus, OrderStatus.CANCELLED, changer);
        clearRelatedCaches(user.getId(), productsToUpdate);
    }

    // 3. GHI LOG (Dùng chung)
    private void saveAuditLog(Order order, OrderStatus oldStatus, OrderStatus newStatus, String changer) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldstatus(oldStatus);
        history.setNewstatus(newStatus);
        history.setUpdatetime(LocalDateTime.now());
        history.setChanger(changer);
        historyRepository.save(history);
    }

    // 4. CLEAR CACHE (Dùng chung)
    private void clearRelatedCaches(Long userId, List<Product> updatedProducts) {
        Cache ordersCache = cacheManager.getCache("orders");
        if (ordersCache != null) ordersCache.evict(userId);

        Cache usersCache = cacheManager.getCache("users");
        if (usersCache != null) usersCache.clear();

        Cache userDetailCache = cacheManager.getCache("user");
        if (userDetailCache != null) userDetailCache.evict(userId);

        if (updatedProducts != null && !updatedProducts.isEmpty()) {
            Cache productDetailCache = cacheManager.getCache("product");
            Cache productsListCache = cacheManager.getCache("products");
            if (productDetailCache != null) updatedProducts.forEach(p -> productDetailCache.evict(p.getId()));
            if (productsListCache != null) productsListCache.clear();
        }
    }
}