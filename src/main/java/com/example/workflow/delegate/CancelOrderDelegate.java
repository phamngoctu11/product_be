package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.nume.OrderStatus; // Đảm bảo import đúng Enum trạng thái của bạn
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("cancelOrderDelegate")
public class CancelOrderDelegate implements JavaDelegate {

    private final OrderRepository orderRepository;
    private final UserVoucherRepository userVoucherRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // 1. Lấy orderId từ Camunda truyền sang
        Long orderId = (Long) execution.getVariable("orderId");

        // 2. Tìm đơn hàng trong Database
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + orderId));

        // 3. Đổi trạng thái thành HỦY BỎ
        order.setStatus(OrderStatus.CANCELLED);
        // (Nếu Status của bạn là String chứ không phải Enum thì dùng: order.setStatus("CANCELLED");)

        // 4. KIỂM TRA VÀ HOÀN LẠI VOUCHER (Rất quan trọng)
        UserVoucher appliedVoucher = order.getUserVoucher();
        if (appliedVoucher != null) {
            appliedVoucher.setUsed(false); // Trả lại trạng thái chưa sử dụng
            appliedVoucher.setUsedDate(null); // Xóa ngày sử dụng
            userVoucherRepository.save(appliedVoucher);
        }

        // 5. Lưu lại đơn hàng đã hủy
        orderRepository.save(order);

        System.out.println(">>> Camunda: Đơn hàng ID " + orderId + " đã bị HỦY TỰ ĐỘNG do hết hàng trong kho. Đã hoàn lại Voucher (nếu có).");
    }
}