package com.example.workflow.service;

import com.example.workflow.dto.OrderDTO;
import com.example.workflow.entity.Order;
import com.example.workflow.mapper.OrderMapper;
import com.example.workflow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    @Cacheable(value = "orders")
    public List<OrderDTO> getOrdersByUserId(Long user_id) {
        List<Order> orders = orderRepository.getOrdersByUserId(user_id);
        return orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }
}