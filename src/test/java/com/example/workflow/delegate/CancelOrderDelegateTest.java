package com.example.workflow.delegate;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.service.ConsultationAttributionService;
import com.example.workflow.service.InventoryReservationService;
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
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final ConsultationAttributionService consultationAttributionService = mock(ConsultationAttributionService.class);
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final CancelOrderDelegate delegate = new CancelOrderDelegate(
            orderRepository,
            userVoucherRepository,
            cacheManager,
            consultationAttributionService,
            inventoryReservationService
    );

    @Test
    void releasesReservationAndDoesNotRestoreAgain() {
        Order order = order();
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(inventoryReservationService.releaseReservedStock(order, "CANCEL_RETURN")).thenReturn(true);

        delegate.execute(execution);

        verify(inventoryReservationService).releaseReservedStock(order, "CANCEL_RETURN");
        verify(inventoryReservationService, never()).restoreDeductedStock(order, "CANCEL_RETURN");
    }

    @Test
    void restoresConfirmedStockWhenThereIsNoReservation() {
        Order order = order();
        when(execution.getVariable("orderId")).thenReturn(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(inventoryReservationService.releaseReservedStock(order, "CANCEL_RETURN")).thenReturn(false);

        delegate.execute(execution);

        verify(inventoryReservationService).restoreDeductedStock(order, "CANCEL_RETURN");
    }

    @Test
    void cancelsOrderRestoresVoucherClearsCachesAndCancelsAttributions() {
        Cache ordersCache = mock(Cache.class);
        Cache pendingOrdersCache = mock(Cache.class);
        Cache productsCache = mock(Cache.class);
        Cache productCache = mock(Cache.class);
        UserVoucher voucher = new UserVoucher();
        voucher.setUsed(true);
        voucher.setUsedDate(LocalDateTime.now());
        Order order = order();
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
        verify(ordersCache).evict("99");
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

    private Order order() {
        User user = new User();
        user.setId("99");
        Order order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setItems(List.of());
        return order;
    }
}
