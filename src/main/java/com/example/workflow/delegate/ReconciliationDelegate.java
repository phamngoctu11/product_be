package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.service.OrderLookupService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("reconciliationDelegate")
@RequiredArgsConstructor
public class ReconciliationDelegate implements JavaDelegate {
    private final OrderLookupService orderLookupService;

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderLookupService.require(orderId);

        if (hasShippingLoss(order)) {
            System.err.println("Shipping loss detected for order #" + orderId);
        } else {
            System.out.println("Reconciliation completed for order #" + orderId);
        }
    }

    private boolean hasShippingLoss(Order order) {
        for (OrderItem item : order.getItems()) {
            if (isLostItem(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLostItem(OrderItem item) {
        return item.getReceivedQuantity() != null
                && item.getExportedQuantity() != null
                && item.getReceivedQuantity() < item.getExportedQuantity();
    }
}
