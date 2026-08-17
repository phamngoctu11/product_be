package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.InventoryReservationService;
import com.example.workflow.service.redis.DeferredCacheEvictionPublisher;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component("deductInventoryDelegate")
public class DeductInventoryDelegate implements JavaDelegate {
    private final OrderRepository orderRepository;
    private final DeferredCacheEvictionPublisher cacheEvictionPublisher;
    private final InventoryReservationService inventoryReservationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId).orElseThrow();
        // New checkout orders already reserve stock. This confirms the reservation as SALE
        // instead of decrementing stock a second time.
        inventoryReservationService.confirmReservation(order);
        orderRepository.save(order);

        execution.setVariable("isStockSufficient", true);
        execution.setVariable("stockDeducted", true);
        execution.setVariable("stockReserved", false);
        clearProductCaches();
    }

    private void clearProductCaches() {
        cacheEvictionPublisher.publishEventually("camunda inventory deducted", List.of(
                CacheEvictionEntry.allEntries("products"),
                CacheEvictionEntry.allEntries("product"),
                CacheEvictionEntry.allEntries("wishlistProducts"),
                CacheEvictionEntry.allEntries("bestSellingProducts")
        ));
    }
}
