package com.example.workflow.service.factory;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.entity.Cart;
import com.example.workflow.entity.User;
import com.example.workflow.nume.Role;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
    public User create(UserCreDTO request, String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(request.getUsername());
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());
        user.setBirth(request.getBirth());
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setAvatarUrl(request.getAvatarUrl());
        user.setReputation(50);
        user.setDelete(false);
        user.setIsActive(true);

        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);
        return user;
    }
}
