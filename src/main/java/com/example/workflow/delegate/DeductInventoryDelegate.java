package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import lombok.AllArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component("deductInventoryDelegate")
public class DeductInventoryDelegate implements JavaDelegate {
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId).orElseThrow();
        boolean isSufficient = true;
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant.getQuantity() < item.getQuantity()) {
                isSufficient = false;
                break;
            }
        }
        if (isSufficient) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getProductVariant();
                variant.setQuantity(variant.getQuantity() - item.getQuantity());
                variantRepository.save(variant);
            }
            execution.setVariable("isStockSufficient", true);
        } else {
            execution.setVariable("isStockSufficient", false);
        }
    }
}