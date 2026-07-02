package com.example.workflow.dto;
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
    private String user_id;
    private String lastname;
    private String customerName;
    private List<OrderItemDTO> items;
    private double totalPrice;
    private double discountAmount;
    private double finalPrice;
    private String voucherName;
    private LocalDateTime startOrderTime;
    private LocalDateTime endOrderTime;
    private OrderStatus status;
    private String cancelReason;
    private String paymentMethod;
    private String note;
    private String approvedById;
    private String approvedByFullName;
}
