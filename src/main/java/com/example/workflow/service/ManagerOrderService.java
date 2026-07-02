package com.example.workflow.service;

import com.example.workflow.dto.AdminReviewRequest;
import com.example.workflow.dto.OrderListDTO;
import com.example.workflow.nume.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerOrderService {
    private final OrderService orderService;

    public Page<OrderListDTO> getPendingOrders(OrderStatus status,Pageable pageable) {
        return orderService.getPendingOrders(status,pageable);
    }

    public void reviewOrder(Long orderId, AdminReviewRequest request, String changerId, String staffId) {
        orderService.processAdminReview(orderId, request, changerId, staffId);
    }

    public void assignStaffToOrder(Long orderId, String staffId) {
        orderService.assignStaffToOrder(orderId, staffId);
    }

    public void kcsCheck(Long orderId, boolean isPassed,String cancelReason) {
        orderService.processManagerKcsCheck(orderId, isPassed,cancelReason);
    }
}
