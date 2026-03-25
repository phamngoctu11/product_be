package com.example.workflow.service;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.dto.OrderStatusHistoryDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.OrderStatusHistory;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.mapper.OrderStatusHistoryMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.OrderStatusHistoryRepository;
import com.example.workflow.repository.ProductVariantRepository;
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
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CacheManager cacheManager;
    private final OrderStatusHistoryMapper historyMapper;

    // ==============================================================
    // ĐÃ SỬA: Lồng ghép logic lấy URL ảnh cho danh sách đơn hàng
    // ==============================================================
    @Cacheable(value = "orders", key = "#user_id")
    public List<OrderDTO> getOrdersByUserId(Long user_id) {
        List<Order> orders = orderRepository.getOrdersByUserId(user_id);
        List<OrderDTO> dtos = orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());

        for (int i = 0; i < orders.size(); i++) {
            injectImageUrls(orders.get(i), dtos.get(i));
        }
        return dtos;
    }

    // ==============================================================
    // ĐÃ SỬA: Lồng ghép logic lấy URL ảnh cho 1 đơn hàng cụ thể
    // ==============================================================
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.getOrdersById(id);
        OrderDTO dto = orderMapper.toDto(order);
        injectImageUrls(order, dto);
        return dto;
    }

    // Hàm hỗ trợ chèn link ảnh (Tránh lặp code)
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
                .stream()
                .map(historyMapper::toDto)
                .collect(Collectors.toList());
    }

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

        List<ProductVariant> variantsToUpdate = new ArrayList<>();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getProductVariant();
                if (variant != null) {
                    variant.setQuantity(variant.getQuantity() + item.getQuantity());
                    variantsToUpdate.add(variant);
                }
            }
            productVariantRepository.saveAll(variantsToUpdate);
        }

        order.setCancelReason(reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setEndOrderTime(LocalDateTime.now());

        orderRepository.save(order);
        userRepository.save(user);

        saveAuditLog(order, oldStatus, OrderStatus.CANCELLED, changer);
        clearRelatedCaches(user.getId(), variantsToUpdate);
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