package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("reconciliationDelegate")
@RequiredArgsConstructor
public class ReconciliationDelegate implements JavaDelegate {
    private final OrderRepository orderRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long orderId = (Long) execution.getVariable("orderId");
        Order order = orderRepository.findById(orderId).orElseThrow();

        boolean isLost = false;
        for (OrderItem item : order.getItems()) {
            // Nếu số lượng khách nhận < số lượng kho xuất ra -> Thất thoát đi đường!
            if (item.getReceivedQuantity() != null && item.getExportedQuantity() != null &&
                    item.getReceivedQuantity() < item.getExportedQuantity()) {
                isLost = true;
                break;
            }
        }

        if (isLost) {
            System.err.println("🚨 BÁO ĐỘNG: Đơn hàng #" + orderId + " bị thất thoát trong quá trình vận chuyển!");
            // (Tương lai có thể viết code trừ lương Shipper ở đây)
        } else {
            System.out.println("✅ Đối soát thành công: Đơn hàng #" + orderId + " giao đủ số lượng.");
        }
    }
}