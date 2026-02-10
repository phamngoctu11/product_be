package com.example.workflow.service;
import com.example.workflow.dto.CartResDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.CartItem;
import com.example.workflow.entity.Product;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.CartMapper;
import com.example.workflow.repository.CartRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.Optional;
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
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
    public void removeFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Cart not found"));
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
    }
    @Transactional(readOnly = true)
    public CartResDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Giỏ hàng trống"));
        return cartMapper.toDTO(cart);
    }
    @Transactional
    public String approve_cart(Long userId){
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"Cart not found!!"));
        for(CartItem item : cart.getItems()) {
            Product pro = item.getProduct();
            if (item.getQuantity() > pro.getQuantity()) {
                return "Thất bại: Sản phẩm " + pro.getProduct_name() + " không đủ số lượng trong kho.";
            }
            pro.setQuantity(pro.getQuantity() - item.getQuantity());
            productRepository.save(pro);
        }
        cart.getItems().clear();
        cartRepository.save(cart);
        return "Duyệt giỏ hàng thành công!";
        }
    }
