package com.example.workflow.delegate;

import com.example.workflow.entity.InventoryTransaction;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.InventoryTransactionRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryTransactionRepository inventoryRepo; // 🚨 THÊM REPO NÀY
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

        order.setStatus(OrderStatus.CANCELLED);
        boolean stockDeducted = Boolean.TRUE.equals(execution.getVariable("stockDeducted"));

        // 🚨 HOÀN TỒN KHO VÀ GHI SỔ CÁI
        if (stockDeducted && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getProductVariant();
                if (variant != null) {
                    // Tăng lại tồn kho trong Variant
                    variant.setQuantity(variant.getQuantity() + item.getQuantity());
                    variantRepository.save(variant);

                    // Ghi Sổ Sao Kê Kho (Loại: Hoàn trả)
                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setProductVariant(variant);
                    tx.setOrder(order);
                    tx.setUser(order.getUser());
                    tx.setQuantityChange(item.getQuantity()); // Hoàn lại kho nên là số DƯƠNG
                    tx.setRemainingStock(variant.getQuantity()); // Tồn kho thực tế lúc đó
                    tx.setTransactionType("CANCEL_RETURN"); // Đánh dấu đây là giao dịch Hủy đơn
                    tx.setCreatedAt(LocalDateTime.now());
                    inventoryRepo.save(tx);
                }
            }
        }

        UserVoucher appliedVoucher = order.getUserVoucher();
        if (appliedVoucher != null) {
            appliedVoucher.setUsed(false);
            appliedVoucher.setUsedDate(null);
            userVoucherRepository.save(appliedVoucher);
        }

        orderRepository.save(order);

        if (cacheManager.getCache("orders") != null) {
            cacheManager.getCache("orders").evict(order.getUser().getId());
        }
        clearCache("pendingOrders");
        clearCache("products");
        clearCache("product");
        System.out.println(">>> Camunda: Đơn hàng bị HỦY -> Đã dọn dẹp kho, ghi sổ sao kê & hoàn voucher cho Order " + orderId);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }
}
