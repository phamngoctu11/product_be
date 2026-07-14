package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component("deductInventoryDelegate")
public class DeductInventoryDelegate implements JavaDelegate {
    private final OrderRepository orderRepository;
    private final CacheManager cacheManager;
    private final InventoryReservationService inventoryReservationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId).orElseThrow();
        inventoryReservationService.confirmReservation(order);
        orderRepository.save(order);

        execution.setVariable("isStockSufficient", true);
        execution.setVariable("stockDeducted", true);
        execution.setVariable("stockReserved", false);
        clearProductCaches();
    }

    private void clearProductCaches() {
        clearCache("products");
        clearCache("product");
        clearCache("bestSellingProducts");
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
