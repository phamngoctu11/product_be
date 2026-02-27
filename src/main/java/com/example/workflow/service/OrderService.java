package com.example.workflow.service;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem; // Thêm import Entity OrderItem
import com.example.workflow.entity.Product; // Thêm import Entity Product
import com.example.workflow.entity.User;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductRepository; // Thêm import ProductRepository
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    public List<OrderDTO> getOrdersByUserId(Long user_id) {
        List<Order> orders = orderRepository.getOrdersByUserId(user_id);
        return orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }
    public OrderDTO getOrderById(Long id){
        return orderMapper.toDto(orderRepository.getOrdersById(id));
    }
    @Transactional
    public void updateStatus(Long id, String newStatusStr, String cancelledReason) {
        Order order = orderRepository.getOrdersById(id);
        if (order == null) {
            throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + id);
        }

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus;

        try {
            newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái mới không hợp lệ: " + newStatusStr);
        }

        // ==========================================
        // LOGIC XỬ LÝ KHI YÊU CẦU HỦY ĐƠN
        // ==========================================
        if (newStatus == OrderStatus.CANCELLED) {
            if (currentStatus != OrderStatus.PENDING_WAREHOUSE) {
                throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi đang ở trạng thái Đang xuất kho.");
            }

            // A. TÍNH TOÁN VÀ TRỪ ĐIỂM UY TÍN
            double totalPrice = order.getTotalPrice();
            int deduction;
            if (totalPrice < 1000000) {
                deduction = 1;
            } else if (totalPrice <= 5000000) {
                deduction = 2;
            } else if (totalPrice <= 10000000) {
                deduction = 3;
            } else {
                deduction = 5;
            }

            User user = order.getUser();
            if (user.getReputation() < deduction) {
                throw new IllegalStateException("Điểm uy tín hiện tại của bạn (" + user.getReputation() + ") không đủ để tự hủy đơn này (cần trừ " + deduction + " điểm). Vui lòng liên hệ CSKH.");
            }
            user.setReputation(user.getReputation() - deduction);
            userRepository.save(user);

            // B. HOÀN TRẢ KHO
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    if (product != null) {
                        int newStock = product.getQuantity() + item.getQuantity();
                        product.setQuantity(newStock);
                        productRepository.save(product);
                    }
                }
            }

            // C. LƯU LÝ DO HỦY VÀ THỜI GIAN
            order.setCancelReason(cancelledReason);
            order.setEndOrderTime(LocalDateTime.now()); // Lưu thời gian kết thúc

        } else {
            // Các logic chặn đổi trạng thái thông thường
            if (currentStatus == OrderStatus.CANCELLED) {
                throw new IllegalStateException("Đơn hàng đã bị hủy, không thể cập nhật.");
            }
            if (currentStatus == OrderStatus.DELIVERED) {
                throw new IllegalStateException("Đơn hàng đã được giao thành công, không thể thay đổi trạng thái.");
            }
            if (currentStatus == OrderStatus.SHIPPING && newStatus == OrderStatus.PENDING_WAREHOUSE) {
                throw new IllegalStateException("Đơn hàng đang giao, không thể quay lại trạng thái xuất kho.");
            }

            // Nếu trạng thái mới là ĐÃ GIAO (DELIVERED) thì cũng lưu thời gian kết thúc
            if (newStatus == OrderStatus.DELIVERED) {
                order.setEndOrderTime(LocalDateTime.now());
            }
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
    }
}