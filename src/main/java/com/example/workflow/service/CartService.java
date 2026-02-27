package com.example.workflow.service;

import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.*;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // Nhớ import thư viện này

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;

    @CacheEvict(value = "carts", key = "#userId")
    public void updateQuantity(Long userId, Long productId, int newQuantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Cart empty"));
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Product not in cart"));
        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }
        cartRepository.save(cart);
    }

    @CacheEvict(value = "carts", key = "#userId")
    public void removeFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Cart not found"));
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#userId", unless = "#result == null")
    public CartResDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Giỏ hàng trống"));
        return cartMapper.toDTO(cart);
    }

    @Caching(evict = {
            @CacheEvict(value = "carts", key = "#userId"),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "orders", key = "#userId")
    })
    @Transactional
    // BỔ SUNG THÊM THAM SỐ List<Long> productIdsToCheckout
    public String approve_cart(Long userId, List<Long> productIdsToCheckout) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy User!"));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ hàng!"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thất bại: Giỏ hàng đang trống.");
        }

        if (productIdsToCheckout == null || productIdsToCheckout.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán.");
        }

        // Lọc ra các Item trong giỏ hàng TRÙNG KHỚP với danh sách ID người dùng đã tích chọn
        List<CartItem> itemsToCheckout = cart.getItems().stream()
                .filter(cartItem -> productIdsToCheckout.contains(cartItem.getProduct().getId()))
                .collect(Collectors.toList());

        if (itemsToCheckout.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Các sản phẩm được chọn không tồn tại trong giỏ hàng.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStartOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING_WAREHOUSE);
        order.setItems(new ArrayList<>());
        double totalPrice = 0;

        // Chỉ duyệt qua danh sách các sản phẩm ĐƯỢC CHỌN thay vì toàn bộ giỏ hàng
        for (CartItem cartItem : itemsToCheckout) {
            Product pro = cartItem.getProduct();

            // Giả định bạn lưu tên sản phẩm là getProduct_name() (tùy theo entity của bạn)
            if (cartItem.getQuantity() > pro.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm (ID: " + pro.getId() + ") không đủ số lượng trong kho.");
            }

            // Trừ kho
            pro.setQuantity(pro.getQuantity() - cartItem.getQuantity());
            productRepository.save(pro);

            // Tạo OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(pro);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(pro.getPrice());

            totalPrice += (cartItem.getQuantity() * pro.getPrice());
            order.getItems().add(orderItem);
        }

        order.setTotalPrice(totalPrice);
        orderRepository.save(order);

        // QUAN TRỌNG: CHỈ xóa các item ĐÃ THANH TOÁN, giữ lại các item người dùng chưa chọn
        cartItemRepository.deleteAll(itemsToCheckout);
        cart.getItems().removeAll(itemsToCheckout);
        cartRepository.save(cart);

        return "Duyệt giỏ hàng thành công! Đơn hàng đã được tạo.";
    }
}