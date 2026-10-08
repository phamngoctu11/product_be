package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderStatusHistory;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryService {
    private final OrderStatusHistoryRepository historyRepository;

    public void record(Order order, OrderStatus oldStatus, OrderStatus newStatus, String actorId) {
        if (order == null || oldStatus == newStatus) {
            return;
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldstatus(oldStatus);
        history.setNewstatus(newStatus);
        history.setUpdatetime(LocalDateTime.now());
        history.setChangerId(actorId);
        historyRepository.save(history);
    }
}
