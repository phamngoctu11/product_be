package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.exception.AppException;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomAgreementTimeoutService {
    private static final List<OrderStatus> TIMEOUT_STATUSES = List.of(
            OrderStatus.PENDING_ASSIGNMENT,
            OrderStatus.DISCUSSING
    );

    private final OrderRepository orderRepository;
    private final OrderCancellationService cancellationService;

    @Value("${workflow.custom-agreement-timeout.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${workflow.custom-agreement-timeout.scan-delay-ms:60000}")
    public void cancelExpiredCustomOrders() {
        List<Long> candidates = orderRepository.findCustomAgreementTimeoutCandidates(
                LocalDateTime.now(), TIMEOUT_STATUSES, PageRequest.of(0, Math.max(1, batchSize))
        );
        for (Long orderId : candidates) {
            cancelIfStillExpired(orderId);
        }
    }

    void cancelIfStillExpired(Long orderId) {
        Order snapshot = orderRepository.findById(orderId).orElse(null);
        if (snapshot == null || snapshot.getUserSubmittedAt() != null || snapshot.getConfirmationDueAt() == null
                || !TIMEOUT_STATUSES.contains(snapshot.getStatus())
                || LocalDateTime.now().isBefore(snapshot.getConfirmationDueAt())) {
            return;
        }
        String dueKey = snapshot.getConfirmationDueAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try {
            cancellationService.cancelBySystem(
                    orderId,
                    snapshot.getVersion(),
                    CancellationSource.CUSTOM_CONFIRMATION_TIMEOUT,
                    "Customer did not submit a valid custom agreement before the confirmation deadline.",
                    "custom-confirm-timeout:" + orderId,
                    "custom-confirm-timeout:" + orderId + ":" + dueKey
            );
        } catch (AppException exception) {
            // A concurrent valid submit or another terminal transition won the order lock.
            log.debug("Skipped custom agreement timeout for order {}: {}", orderId, exception.getMessage());
        }
    }
}
