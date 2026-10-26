package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderAgreementRepository;
import com.example.workflow.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAgreementTimeoutServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderAgreementRepository agreementRepository;
    @Mock private OrderCancellationService cancellationService;

    @Test
    void expiredUnsubmittedCustomOrderUsesSharedCancellationKernel() {
        Order order = new Order();
        order.setId(41L);
        order.setVersion(7L);
        order.setStatus(OrderStatus.DISCUSSING);
        order.setConfirmationDueAt(LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findById(41L)).thenReturn(Optional.of(order));
        CustomAgreementTimeoutService service = new CustomAgreementTimeoutService(orderRepository, agreementRepository, cancellationService);

        service.cancelIfStillExpired(41L);

        verify(cancellationService).cancelBySystem(
                eq(41L), eq(7L), eq(CancellationSource.CUSTOM_CONFIRMATION_TIMEOUT),
                anyString(), eq("custom-confirm-timeout:41"), startsWith("custom-confirm-timeout:41:")
        );
    }

    @Test
    void firstValidSubmissionMakesTimeoutNoOpEvenWhenDueHasPassed() {
        Order order = new Order();
        order.setId(41L);
        order.setVersion(8L);
        order.setStatus(OrderStatus.DISCUSSING);
        order.setConfirmationDueAt(LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findById(41L)).thenReturn(Optional.of(order));
        when(agreementRepository.existsByOrderIdAndSubmittedAtIsNotNull(41L)).thenReturn(true);
        CustomAgreementTimeoutService service = new CustomAgreementTimeoutService(orderRepository, agreementRepository, cancellationService);

        service.cancelIfStillExpired(41L);

        verifyNoInteractions(cancellationService);
    }
}
