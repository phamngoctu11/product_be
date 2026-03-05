package com.example.workflow.dto;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.nume.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderDTO implements Serializable {
    private Long id;
    private Long user_id;
    private List<OrderItemDTO> items;
    private double totalPrice;
    private double discountAmount;  // Số tiền được giảm
    private double finalPrice;      // Thực trả

    private String voucherName;
    LocalDateTime startOrderTime;
    LocalDateTime endOrderTime;
    OrderStatus status;
    String cancelReason;
}
