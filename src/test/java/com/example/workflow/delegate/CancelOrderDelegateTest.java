package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.repository.InventoryTransactionRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.repository.UserVoucherRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelOrderDelegateTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final UserVoucherRepository userVoucherRepository = mock(UserVoucherRepository.class);
    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final InventoryTransactionRepository inventoryRepository = mock(InventoryTransactionRepository.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final CancelOrderDelegate delegate = new CancelOrderDelegate(
            orderRepository,
            userVoucherRepository,
            variantRepository,
            inventoryRepository,
            cacheManager
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
    }

    private Order orderWithItems(OrderItem... items) {
        User user = new User();
        user.setId(99L);
        Order order = new Order();
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
