package com.example.workflow.service;

import com.example.workflow.dto.ItemCheckRequest;
import com.example.workflow.dto.OrderListDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffOrderService {
    private final OrderService orderService;

    public Page<OrderListDTO> getWarehousePendingOrders(Pageable pageable) {
        return orderService.getWarehousePendingOrders(pageable);
    }

    public Page<OrderListDTO> getMyAssignedStaffOrders(Pageable pageable) {
        return orderService.getMyAssignedStaffOrders(pageable);
    }

    public void claimWarehouseOrder(Long orderId) {
        orderService.claimWarehouseOrder(orderId);
    }

    public void exportOrder(Long orderId, List<ItemCheckRequest> exportData) {
        orderService.processStaffExport(orderId, exportData);
    }
}
