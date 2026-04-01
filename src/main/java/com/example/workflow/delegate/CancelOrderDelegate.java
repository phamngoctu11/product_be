package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
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

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

        // 1. Cập nhật trạng thái
        order.setStatus(OrderStatus.CANCELLED);

        // 2. Hoàn Tồn kho
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getProductVariant();
                if (variant != null) {
                    variant.setQuantity(variant.getQuantity() + item.getQuantity());
                    variantRepository.save(variant);
                }
            }
        }

        // 3. Hoàn Voucher
        UserVoucher appliedVoucher = order.getUserVoucher();
        if (appliedVoucher != null) {
            appliedVoucher.setUsed(false);
            appliedVoucher.setUsedDate(null);
            userVoucherRepository.save(appliedVoucher);
        }

        orderRepository.save(order);

        // 4. Xóa cache để Admin thấy danh sách được cập nhật
        if (cacheManager.getCache("orders") != null) {
            cacheManager.getCache("orders").evict(order.getUser().getId());
        }
        System.out.println(">>> Camunda: ADMIN từ chối -> Đã dọn dẹp kho & voucher cho Order " + orderId);
    }
}