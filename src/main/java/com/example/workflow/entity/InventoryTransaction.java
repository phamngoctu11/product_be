package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_transaction")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Biến thể sản phẩm nào bị ảnh hưởng?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    // Thuộc Đơn hàng nào? (Có thể null nếu là giao dịch Nhập kho/Kiểm kê)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // Khách hàng liên quan (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Biến động kho (VD: -2 nếu bán ra, +50 nếu nhập thêm)
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    // 🚨 RẤT QUAN TRỌNG: Tồn kho thực tế còn lại ngay tại khoảnh khắc giao dịch thành công
    @Column(name = "remaining_stock", nullable = false)
    private int remainingStock;

    // Phân loại: "SALE" (Bán), "RESTOCK" (Nhập kho), "RETURN" (Khách trả lại), "LOST" (Mất hàng đi đường)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    // Thời gian chốt xuất kho
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Thời gian khách hàng xác nhận nhận hàng (Để biết chu kỳ 1 đơn hàng)
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // Tự động gán giờ chốt sao kê
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}