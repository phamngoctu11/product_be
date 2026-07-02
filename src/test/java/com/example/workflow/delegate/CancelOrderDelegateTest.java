package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.service.ConsultationAttributionService;
import com.example.workflow.service.InventoryTransactionService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelOrderDelegateTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final UserVoucherRepository userVoucherRepository = mock(UserVoucherRepository.class);
    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final ConsultationAttributionService consultationAttributionService = mock(ConsultationAttributionService.class);
    private final InventoryTransactionService inventoryTransactionService = mock(InventoryTransactionService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final CancelOrderDelegate delegate = new CancelOrderDelegate(
            orderRepository,
            userVoucherRepository,
            variantRepository,
            cacheManager,
            consultationAttributionService,
            inventoryTransactionService
    );

    @Test
    void doesNotRestoreStockWhenStockWasNotDeducted() throws Exception {
        ProductVariant variant = variant(1L, 5);
        Order order = orderWithItems(item(variant, 2));
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(execution.getVariable("stockDeducted")).thenReturn(false);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        assertThat(variant.getQuantity()).isEqualTo(5);
        verify(variantRepository, never()).save(variant);
        verify(inventoryTransactionService, never()).record(order, variant, 2, "CANCEL_RETURN");
    }

    @Test
    void restoresStockWhenStockWasDeducted() throws Exception {
        ProductVariant variant = variant(1L, 5);
        Order order = orderWithItems(item(variant, 2));
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(execution.getVariable("stockDeducted")).thenReturn(true);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        assertThat(variant.getQuantity()).isEqualTo(7);
        verify(variantRepository).save(variant);
        verify(inventoryTransactionService).record(order, variant, 2, "CANCEL_RETURN");
    }

    @Test
    void cancelsOrderRestoresVoucherClearsCachesAndCancelsAttributions() throws Exception {
        Cache ordersCache = mock(Cache.class);
        Cache pendingOrdersCache = mock(Cache.class);
        Cache productsCache = mock(Cache.class);
        Cache productCache = mock(Cache.class);
        UserVoucher voucher = new UserVoucher();
        voucher.setUsed(true);
        voucher.setUsedDate(LocalDateTime.now());
        Order order = orderWithItems();
        order.setUserVoucher(voucher);
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(cacheManager.getCache("orders")).thenReturn(ordersCache);
        when(cacheManager.getCache("pendingOrders")).thenReturn(pendingOrdersCache);
        when(cacheManager.getCache("products")).thenReturn(productsCache);
        when(cacheManager.getCache("product")).thenReturn(productCache);

        delegate.execute(execution);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(voucher.isUsed()).isFalse();
        assertThat(voucher.getUsedDate()).isNull();
        verify(userVoucherRepository).save(voucher);
        verify(orderRepository).save(order);
        verify(consultationAttributionService).cancelOrderAttributions(10L);
        verify(ordersCache).evict(99L);
        verify(pendingOrdersCache).clear();
        verify(productsCache).clear();
        verify(productCache).clear();
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Order not found: 404");

        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Order orderWithItems(OrderItem... items) {
        User user = new User();
        user.setId("99");
        Order order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setItems(List.of(items));
        return order;
    }

    private ProductVariant variant(Long variantId, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setQuantity(stock);
        return variant;
    }

    private OrderItem item(ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }
}
