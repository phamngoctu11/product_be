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

    // Xóa item khỏi giỏ hàng cũng cần xóa cache
    @CacheEvict(value = "carts", key = "#userId")
    public void removeFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Cart not found"));
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
    }

    // Lưu kết quả giỏ hàng vào Redis với key là userId
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#userId", unless = "#result == null")
    public CartResDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Giỏ hàng trống"));
        return cartMapper.toDTO(cart);
    }
    @Caching(evict = {
            @CacheEvict(value = "carts", key = "#userId"),
            @CacheEvict(value = "products", allEntries = true), // Xóa hết cache sản phẩm vì số lượng tồn kho đã thay đổi
            @CacheEvict(value = "users", allEntries = true)     // Nếu User có chứa list đơn hàng hoặc reputation bị thay đổi
    })
    @Transactional
    public String approve_cart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy User!"));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ hàng!"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "Thất bại: Giỏ hàng đang trống.";
        }
        Order order = new Order();
        order.setUser(user);
        order.setStartOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING_WAREHOUSE);
        order.setItems(new ArrayList<>());
        double totalPrice = 0;
        for (CartItem cartItem : cart.getItems()) {
            Product pro = cartItem.getProduct();
            if (cartItem.getQuantity() > pro.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm " + pro.getProduct_name() + " không đủ số lượng trong kho.");
            }
            pro.setQuantity(pro.getQuantity() - cartItem.getQuantity());
            productRepository.save(pro);
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
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        return "Duyệt giỏ hàng thành công! Đơn hàng đã được tạo.";
    }
}