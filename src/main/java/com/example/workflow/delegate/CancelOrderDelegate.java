package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.OrderCancellationService;
import com.example.workflow.service.OrderLookupService;
import com.example.workflow.service.cache.ApplicationCacheService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final OrderLookupService orderLookupService;
    private final OrderCancellationService cancellationService;
    private final ApplicationCacheService applicationCacheService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderLookupService.requireForUpdate(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        cancellationService.cancel(
                order,
                new OrderCancellationService.Request(
                        order.getCancelReason(),
                        "CANCEL_RETURN",
                        true,
                        null,
                        CancellationSource.SYSTEM,
                        "camunda:" + execution.getProcessInstanceId(),
                        false,
                        null
                )
        );

        applicationCacheService.evictCamundaOrderCancelled(order, oldStatus);
    }
}
