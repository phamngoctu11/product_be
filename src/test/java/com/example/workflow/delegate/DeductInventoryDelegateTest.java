package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.repository.InventoryTransactionRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeductInventoryDelegateTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final InventoryTransactionRepository inventoryRepository = mock(InventoryTransactionRepository.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final DeductInventoryDelegate delegate = new DeductInventoryDelegate(
            orderRepository,
            variantRepository,
            cacheManager,
            inventoryRepository
    );

    @Test
    void restoresAlreadyDeductedItemsWhenLaterItemFails() {
        ProductVariant firstVariant = variant(1L, 5);
        ProductVariant secondVariant = variant(2L, 1);
        Order order = orderWithItems(item(firstVariant, 2), item(secondVariant, 3));
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        assertThat(firstVariant.getQuantity()).isEqualTo(5);
        assertThat(secondVariant.getQuantity()).isEqualTo(1);
        verify(execution).setVariable("isStockSufficient", false);
        verify(execution).setVariable("stockDeducted", false);
    }

    @Test
    void marksStockDeductedWhenAllItemsSucceed() {
        ProductVariant firstVariant = variant(1L, 5);
        ProductVariant secondVariant = variant(2L, 8);
        Order order = orderWithItems(item(firstVariant, 2), item(secondVariant, 3));
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        assertThat(firstVariant.getQuantity()).isEqualTo(3);
        assertThat(secondVariant.getQuantity()).isEqualTo(5);
        verify(execution).setVariable("isStockSufficient", true);
        verify(execution).setVariable("stockDeducted", true);
    }

    private Order orderWithItems(OrderItem... items) {
        Order order = new Order();
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
