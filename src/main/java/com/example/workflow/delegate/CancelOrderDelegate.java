package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.OrderLookupService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final OrderLookupService orderLookupService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderLookupService.requireForUpdate(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        throw new IllegalStateException(
                "Camunda cannot cancel Order directly. The authenticated manager/system application service "
                        + "must commit cancellation before this legacy task is reached. Process="
                        + execution.getProcessInstanceId()
        );
    }
}
