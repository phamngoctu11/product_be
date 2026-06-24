package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.service.ConsultationAttributionService;
import com.example.workflow.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final ProductVariantRepository variantRepository;
    private final CacheManager cacheManager;
    private final ConsultationAttributionService consultationAttributionService;
    private final InventoryTransactionService inventoryTransactionService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(OrderStatus.CANCELLED);
        boolean stockDeducted = Boolean.TRUE.equals(execution.getVariable("stockDeducted"));

        if (stockDeducted && order.getItems() != null) {
            restoreStock(order);
        }

        restoreVoucher(order.getUserVoucher());

        orderRepository.save(order);
        consultationAttributionService.cancelOrderAttributions(order.getId());

        if (cacheManager.getCache("orders") != null) {
            cacheManager.getCache("orders").evict(order.getUser().getId());
        }
        clearCache("pendingOrders");
        clearCache("products");
        clearCache("product");
        System.out.println(">>> Camunda: Order cancelled and related stock/voucher data restored for order " + orderId);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant == null) {
                continue;
            }

            variant.setQuantity(variant.getQuantity() + item.getQuantity());
            variantRepository.save(variant);
            inventoryTransactionService.record(order, variant, item.getQuantity(), "CANCEL_RETURN");
        }
    }

    private void restoreVoucher(UserVoucher appliedVoucher) {
        if (appliedVoucher == null) {
            return;
        }
        appliedVoucher.setUsed(false);
        appliedVoucher.setUsedDate(null);
        userVoucherRepository.save(appliedVoucher);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
