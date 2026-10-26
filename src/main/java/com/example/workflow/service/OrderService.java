package com.example.workflow.service;

import com.example.workflow.dto.*;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.entity.*;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.OrderStatusHistoryMapper;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.*;
import com.example.workflow.service.assembler.OrderDetailsAssembler;
import com.example.workflow.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderStatusHistoryMapper historyMapper;
    private final CurrentUserService currentUserService;
    private final OrderDetailsAssembler orderDetailsAssembler;
    private final OrderLookupService orderLookupService;
    private final OrderLookupTokenService orderLookupTokenService;

    private void assertCurrentUserCanViewOrder(Order order) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == Role.USER
                && (order.getUser() == null || !order.getUser().getId().equals(currentUser.getId()))) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.USER_DATA_ACCESS_FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderLookupService.require(id);
        assertCurrentUserCanViewOrder(order);
        return orderDetailsAssembler.toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getOrderHistory(Long orderId) {
        Order order = orderLookupService.require(orderId);
        assertCurrentUserCanViewOrder(order);
        return loadOrderHistory(orderId);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USER_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).userOrders(@currentUserService.requireCurrentUserId(), #minPrice, #maxPrice, #pageable)"
    )
    public Page<OrderListDTO> getMyOrders(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        String userId = currentUserService.requireCurrentUserId();
        return orderRepository.findListDtoByUserId(
                userId,
                List.of(
                        OrderStatus.PENDING_APPROVAL,
                        OrderStatus.PENDING_ASSIGNMENT,
                        OrderStatus.DISCUSSING,
                        OrderStatus.WAITING_STAFF_CONFIRMATION,
                        OrderStatus.ORDER_ACCEPTED,
                        OrderStatus.ORDER_CREATING,
                        OrderStatus.READY_TO_SHIP
                ),
                OrderStatus.SHIPPING,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                minPrice,
                maxPrice,
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USER_CANCELLED_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).userCancelledOrders(@currentUserService.requireCurrentUserId(), #minPrice, #maxPrice, #pageable)"
    )
    public Page<OrderListDTO> getMyCancelledOrders(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        String userId = currentUserService.requireCurrentUserId();
        return orderRepository.findListDtoByUserIdAndStatus(
                userId,
                OrderStatus.CANCELLED,
                OrderStatus.DELIVERED,
                minPrice,
                maxPrice,
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.STAFF_ASSIGNED_ORDERS,
            key = "T(com.example.workflow.cache.CacheKeys).staffAssignedOrders(@currentUserService.requireCurrentUserId(), #pageable)"
    )
    public Page<OrderListDTO> getMyAssignedStaffOrders(Pageable pageable) {
        User staff = currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
        return orderRepository.findListDtoByWarehouseStaffIdAndStatusIn(
                staff.getId(),
                List.of(
                        OrderStatus.DISCUSSING,
                        OrderStatus.WAITING_STAFF_CONFIRMATION,
                        OrderStatus.ORDER_ACCEPTED,
                        OrderStatus.ORDER_CREATING,
                        OrderStatus.READY_TO_SHIP,
                        OrderStatus.SHIPPING,
                        OrderStatus.DELIVERED,
                        OrderStatus.CANCELLED
                ),
                List.of(
                        OrderStatus.DISCUSSING,
                        OrderStatus.WAITING_STAFF_CONFIRMATION,
                        OrderStatus.ORDER_ACCEPTED,
                        OrderStatus.ORDER_CREATING
                ),
                OrderStatus.DISCUSSING,
                OrderStatus.ORDER_CREATING,
                OrderStatus.READY_TO_SHIP,
                PageableUtils.normalize(pageable, 20, 100)
        );
    }

    @Transactional(readOnly = true)
    public OrderDTO getGuestOrder(Long orderId, String token) {
        Order order = orderLookupService.require(orderId);
        orderLookupTokenService.authorize(order, token, OrderLookupTokenService.Scope.READ);
        return orderDetailsAssembler.toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getGuestOrderHistory(Long orderId, String token) {
        Order order = orderLookupService.require(orderId);
        orderLookupTokenService.authorize(order, token, OrderLookupTokenService.Scope.READ);
        return loadOrderHistory(orderId);
    }

    private List<OrderStatusHistoryDTO> loadOrderHistory(Long orderId) {
        return historyRepository.findByOrderIdOrderByUpdatetimeAsc(orderId)
                .stream().map(historyMapper::toDto).collect(Collectors.toList());
    }

}

