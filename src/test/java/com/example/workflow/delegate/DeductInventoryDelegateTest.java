package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductVariantRepository;
import com.example.workflow.service.InventoryTransactionService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("DeductInventoryDelegate Tests")
class DeductInventoryDelegateTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final InventoryTransactionService inventoryTransactionService = mock(InventoryTransactionService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);

    private final DeductInventoryDelegate delegate = new DeductInventoryDelegate(
            orderRepository,
            variantRepository,
            cacheManager,
            inventoryTransactionService
    );

    @Test
    @DisplayName("Should restore already deducted items when later item fails")
    void restoresAlreadyDeductedItemsWhenLaterItemFails() {
        // Arrange
        ProductVariant firstVariant = variant(1L, 5);
        ProductVariant secondVariant = variant(2L, 1);
        Order order = orderWithItems(item(firstVariant, 2), item(secondVariant, 3));

        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // Act
        delegate.execute(execution);

        // Assert
        assertThat(firstVariant.getQuantity()).isEqualTo(5);
        assertThat(secondVariant.getQuantity()).isEqualTo(1);
        verify(execution).setVariable("isStockSufficient", false);
        verify(execution).setVariable("stockDeducted", false);
        verify(inventoryTransactionService).record(order, firstVariant, -2, "SALE");
        verify(inventoryTransactionService).record(null, firstVariant, 2, "ROLLBACK");
    }

    @Test
    @DisplayName("Should mark stock deducted when all items succeed")
    void marksStockDeductedWhenAllItemsSucceed() {
        // Arrange
        ProductVariant firstVariant = variant(1L, 5);
        ProductVariant secondVariant = variant(2L, 8);
        Order order = orderWithItems(item(firstVariant, 2), item(secondVariant, 3));

        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // Act
        delegate.execute(execution);

        // Assert
        assertThat(firstVariant.getQuantity()).isEqualTo(3);
        assertThat(secondVariant.getQuantity()).isEqualTo(5);
        verify(execution).setVariable("isStockSufficient", true);
        verify(execution).setVariable("stockDeducted", true);
        verify(inventoryTransactionService).record(order, firstVariant, -2, "SALE");
        verify(inventoryTransactionService).record(order, secondVariant, -3, "SALE");
    }

    @Test
    @DisplayName("Should deduct exported quantity when present")
    void deductsExportedQuantityWhenPresent() {
        ProductVariant variant = variant(1L, 5);
        OrderItem item = item(variant, 4);
        item.setExportedQuantity(2);
        Order order = orderWithItems(item);

        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        assertThat(variant.getQuantity()).isEqualTo(3);
        verify(execution).setVariable("isStockSufficient", true);
        verify(execution).setVariable("stockDeducted", true);
        verify(inventoryTransactionService).record(order, variant, -2, "SALE");
    }

    @Test
    @DisplayName("Should skip item when exported quantity is zero")
    void skipsItemWhenExportedQuantityIsZero() {
        ProductVariant variant = variant(1L, 5);
        OrderItem item = item(variant, 4);
        item.setExportedQuantity(0);
        Order order = orderWithItems(item);

        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        delegate.execute(execution);

        assertThat(variant.getQuantity()).isEqualTo(5);
        verify(variantRepository, never()).save(any());
        verifyNoInteractions(inventoryTransactionService);
        verify(execution).setVariable("isStockSufficient", true);
        verify(execution).setVariable("stockDeducted", true);
    }

    @Test
    @DisplayName("Should throw when order does not exist")
    void throwsWhenOrderDoesNotExist() {
        when(execution.getVariable("orderId")).thenReturn(404L);
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(java.util.NoSuchElementException.class);

        verify(variantRepository, never()).save(any());
        verifyNoInteractions(inventoryTransactionService);
    }

    // Helper methods
    private Order orderWithItems(OrderItem... items) {
        Order order = new Order();
        order.setId(10L);
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
