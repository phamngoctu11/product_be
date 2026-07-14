package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InventoryReservationServiceTest {
    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final InventoryTransactionService transactionService = mock(InventoryTransactionService.class);
    private final InventoryReservationService service = new InventoryReservationService(
            variantRepository,
            transactionService
    );

    @Test
    void reservesStockWithConditionalDatabaseUpdate() {
        ProductVariant variant = variant(11L);
        Order order = order(item(variant, 1));
        when(variantRepository.decreaseStockIfAvailable(11L, 1)).thenReturn(1);

        service.reserve(order);

        assertThat(order.isStockReserved()).isTrue();
        verify(variantRepository).decreaseStockIfAvailable(11L, 1);
    }

    @Test
    void rejectsCheckoutWhenConditionalUpdateCannotReserveStock() {
        ProductVariant variant = variant(11L);
        Order order = order(item(variant, 1));
        when(variantRepository.decreaseStockIfAvailable(11L, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.reserve(order)).isInstanceOf(AppException.class);

        assertThat(order.isStockReserved()).isFalse();
    }

    @Test
    void onlyOneConcurrentCheckoutCanReserveTheLastUnit() throws Exception {
        AtomicInteger stock = new AtomicInteger(1);
        when(variantRepository.decreaseStockIfAvailable(11L, 1))
                .thenAnswer(invocation -> stock.compareAndSet(1, 0) ? 1 : 0);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> attemptReservation(order(item(variant(11L), 1)), start));
            Future<Boolean> second = executor.submit(() -> attemptReservation(order(item(variant(11L), 1)), start));
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(stock.get()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void confirmsExistingReservationWithoutSecondStockDecrease() {
        Order order = order(item(variant(11L), 1));
        order.setStockReserved(true);

        service.confirmReservation(order);

        assertThat(order.isStockReserved()).isFalse();
        assertThat(order.isStockDeducted()).isTrue();
        verify(transactionService).updateOrderTransactionType(order, "RESERVATION", "SALE");
        verify(variantRepository, never()).decreaseStockIfAvailable(11L, 1);
    }

    @Test
    void repeatedConfirmationIsIdempotent() {
        Order order = order(item(variant(11L), 1));
        order.setStockDeducted(true);

        service.confirmReservation(order);

        verifyNoInteractions(variantRepository, transactionService);
    }

    @Test
    void cancellationReturnsReservedStockOnlyOnce() {
        ProductVariant variant = variant(11L);
        Order order = order(item(variant, 1));
        order.setStockReserved(true);
        when(variantRepository.findStockById(11L)).thenReturn(Optional.of(5));

        boolean firstRelease = service.releaseReservedStock(order, "CANCEL_RETURN");
        boolean secondRelease = service.releaseReservedStock(order, "CANCEL_RETURN");

        assertThat(firstRelease).isTrue();
        assertThat(secondRelease).isFalse();
        assertThat(order.isStockReserved()).isFalse();
        verify(variantRepository).increaseStock(11L, 1);
        verify(transactionService).updateOrderTransactionType(
                order,
                "RESERVATION",
                "RESERVATION_CANCELLED"
        );
        verify(transactionService).record(
                order,
                variant,
                order.getUser(),
                1,
                5,
                "CANCEL_RETURN"
        );
    }

    @Test
    void legacyOrderStillUsesAtomicDeduction() {
        ProductVariant variant = variant(11L);
        OrderItem item = item(variant, 3);
        item.setExportedQuantity(2);
        Order order = order(item);
        when(variantRepository.decreaseStockIfAvailable(11L, 2)).thenReturn(1);
        when(variantRepository.findStockById(11L)).thenReturn(Optional.of(4));

        service.confirmReservation(order);

        assertThat(order.isStockDeducted()).isTrue();
        verify(variantRepository).decreaseStockIfAvailable(11L, 2);
        verify(transactionService).record(
                order,
                variant,
                order.getUser(),
                -2,
                4,
                "SALE"
        );
    }

    private Order order(OrderItem... items) {
        User user = new User();
        user.setId("user-1");
        Order order = new Order();
        order.setId(100L);
        order.setUser(user);
        order.setItems(List.of(items));
        return order;
    }

    private boolean attemptReservation(Order order, CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            service.reserve(order);
            return true;
        } catch (AppException exception) {
            return false;
        }
    }

    private ProductVariant variant(Long id) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        return variant;
    }

    private OrderItem item(ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }
}
