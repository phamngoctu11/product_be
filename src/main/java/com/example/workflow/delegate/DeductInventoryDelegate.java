package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component("deductInventoryDelegate")
public class DeductInventoryDelegate implements JavaDelegate {
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final CacheManager cacheManager;
    private final InventoryTransactionService inventoryTransactionService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId).orElseThrow();
        List<OrderItem> deductedItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            int quantityToDeduct = resolveExportedQuantity(item);

            if (quantityToDeduct == 0) {
                continue;
            }

            if (variant.getQuantity() < quantityToDeduct) {
                restoreDeductedItems(deductedItems);
                execution.setVariable("isStockSufficient", false);
                execution.setVariable("stockDeducted", false);
                return;
            }

            variant.setQuantity(variant.getQuantity() - quantityToDeduct);
            variantRepository.save(variant);
            inventoryTransactionService.record(order, variant, -quantityToDeduct, "SALE");

            deductedItems.add(item);
        }

        execution.setVariable("isStockSufficient", true);
        execution.setVariable("stockDeducted", true);
        clearProductCaches();
    }

    private void restoreDeductedItems(List<OrderItem> deductedItems) {
        for (OrderItem item : deductedItems) {
            ProductVariant variant = item.getProductVariant();
            int quantityToRestore = resolveExportedQuantity(item);

            variant.setQuantity(variant.getQuantity() + quantityToRestore);
            variantRepository.save(variant);
            inventoryTransactionService.record(null, variant, quantityToRestore, "ROLLBACK");
        }
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

    private int resolveExportedQuantity(OrderItem item) {
        Integer exportedQuantity = item.getExportedQuantity();
        return exportedQuantity == null ? item.getQuantity() : Math.max(exportedQuantity, 0);
    }
}
