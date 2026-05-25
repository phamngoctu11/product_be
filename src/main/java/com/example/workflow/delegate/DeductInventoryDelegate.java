package com.example.workflow.delegate;

import com.example.workflow.entity.InventoryTransaction;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.repository.InventoryTransactionRepository; // Nhớ tạo interface Repo này nhé
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component("deductInventoryDelegate")
public class DeductInventoryDelegate implements JavaDelegate {
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryTransactionRepository inventoryRepo; // 🚨 BƠM SỔ CÁI VÀO ĐÂY

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId).orElseThrow();
        List<OrderItem> deductedItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();

            // 1. Kiểm tra tồn kho
            if (variant.getQuantity() < item.getQuantity()) {
                restoreDeductedItems(deductedItems);
                execution.setVariable("isStockSufficient", false);
                execution.setVariable("stockDeducted", false);
                return; // Thiếu hàng -> Rollback toàn bộ
            }

            // 2. Trừ kho trực tiếp
            variant.setQuantity(variant.getQuantity() - item.getQuantity());
            variantRepository.save(variant);

            // 3. 🚨 GHI VÀO SỔ CÁI SAO KÊ KHO (INVENTORY TRANSACTION)
            InventoryTransaction tx = new InventoryTransaction();
            tx.setProductVariant(variant);
            tx.setOrder(order);
            tx.setUser(order.getUser());
            tx.setQuantityChange(-item.getQuantity()); // Bán ra nên là số ÂM
            tx.setRemainingStock(variant.getQuantity()); // Tồn kho thực tế lúc đó
            tx.setTransactionType("SALE"); // Loại giao dịch
            tx.setCreatedAt(LocalDateTime.now());
            inventoryRepo.save(tx);

            deductedItems.add(item);
        }

        execution.setVariable("isStockSufficient", true);
        execution.setVariable("stockDeducted", true);
    }

    private void restoreDeductedItems(List<OrderItem> deductedItems) {
        // Nếu thiếu hàng giữa chừng, phải hoàn lại và GHI SỔ CÁI HOÀN TRẢ
        for (OrderItem item : deductedItems) {
            ProductVariant variant = item.getProductVariant();
            variant.setQuantity(variant.getQuantity() + item.getQuantity());
            variantRepository.save(variant);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setProductVariant(variant);
            tx.setQuantityChange(item.getQuantity()); // Hoàn lại nên là số DƯƠNG
            tx.setRemainingStock(variant.getQuantity());
            tx.setTransactionType("ROLLBACK");
            inventoryRepo.save(tx);
        }
    }
}