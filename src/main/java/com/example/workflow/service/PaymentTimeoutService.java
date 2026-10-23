package com.example.workflow.service;

import com.example.workflow.entity.Order;
import com.example.workflow.entity.PaymentAttempt;
import com.example.workflow.nume.CancellationSource;
import com.example.workflow.nume.PaymentAttemptStatus;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.PaymentAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PaymentTimeoutService {
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final OrderCancellationService cancellationService;
    private final TransactionTemplate transactionTemplate;

    @Value("${sepay.payment.timeout-batch-size:100}")
    private int batchSize;

    public PaymentTimeoutService(
            PaymentAttemptRepository paymentAttemptRepository,
            OrderRepository orderRepository,
            OrderCancellationService cancellationService,
            PlatformTransactionManager transactionManager
    ) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.orderRepository = orderRepository;
        this.cancellationService = cancellationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${sepay.payment.timeout-scan-delay-ms:60000}")
    public void expireDuePayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = paymentAttemptRepository.findDueAttemptIds(
                PaymentAttemptStatus.PENDING,
                now,
                PageRequest.of(0, Math.max(1, batchSize))
        );
        int expired = 0;
        for (Long id : ids) {
            Boolean changed = transactionTemplate.execute(status -> expireWithinTransaction(id, LocalDateTime.now()));
            if (Boolean.TRUE.equals(changed)) expired++;
        }
        if (expired > 0) log.info("Expired {} SePay payment attempts", expired);
    }

    public boolean expireIfStillDue(Long attemptId) {
        return Boolean.TRUE.equals(transactionTemplate.execute(
                status -> expireWithinTransaction(attemptId, LocalDateTime.now())
        ));
    }

    boolean expireWithinTransaction(Long attemptId, LocalDateTime now) {
        PaymentAttempt attempt = paymentAttemptRepository.findByIdForUpdate(attemptId).orElse(null);
        if (attempt == null
                || attempt.getStatus() != PaymentAttemptStatus.PENDING
                || attempt.getDueAt().isAfter(now)) {
            return false;
        }
        Order order = orderRepository.findByIdForUpdate(attempt.getOrderId()).orElse(null);
        if (order == null || order.getPaymentStatus() == PaymentStatus.PAID) {
            attempt.setReconciliationRequired(true);
            return false;
        }

        attempt.setStatus(PaymentAttemptStatus.EXPIRED);
        order.setPaymentStatus(PaymentStatus.EXPIRED);
        orderRepository.saveAndFlush(order);
        cancellationService.cancelBySystem(
                order.getId(),
                order.getVersion(),
                CancellationSource.PAYMENT_TIMEOUT,
                "Online payment expired.",
                "payment-timeout:" + attempt.getId(),
                "payment-timeout:" + attempt.getId()
        );
        return true;
    }
}
