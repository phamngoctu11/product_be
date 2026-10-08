package com.example.workflow.delegate;

import com.example.workflow.service.OrderLookupService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("deductInventoryDelegate")
public class DeductInventoryDelegate implements JavaDelegate {
    private final OrderLookupService orderLookupService;

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        orderLookupService.require(orderId);
        // Transitional compatibility step. The BPMN node is removed in the
        // inventory cleanup batch; made-to-order orders must never deduct stock.
        execution.setVariable("isStockSufficient", true);
    }
}
