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
    private CustomerInfo customer;
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
    private String email;
    private String guestSessionId;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String approvedById;
    private String approvedByFullName;

    @Data
    @NoArgsConstructor
    public static class CustomerInfo implements Serializable {
        private boolean guest;
        private String userId;
        private String guestSessionId;
        private String name;
        private String email;
        private String phone;
        private String address;

        public CustomerInfo(
                boolean guest,
                String userId,
                String guestSessionId,
                String name,
                String email,
                String phone,
                String address
        ) {
            this.guest = guest;
            this.userId = userId;
            this.guestSessionId = guestSessionId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
        }

        public static CustomerInfo user(String userId, String name, String email, String phone, String address) {
            return new CustomerInfo(false, userId, null, name, email, phone, address);
        }

        public static CustomerInfo guest(String guestSessionId, String name, String email, String phone, String address) {
            return new CustomerInfo(true, null, guestSessionId, name, email, phone, address);
        }
    }
}
