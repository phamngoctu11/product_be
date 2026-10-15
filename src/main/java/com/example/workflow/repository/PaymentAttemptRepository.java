package com.example.workflow.repository;

import com.example.workflow.entity.PaymentAttempt;
import com.example.workflow.nume.PaymentAttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentAttempt p WHERE p.provider = :provider AND p.requestReference = :reference")
    Optional<PaymentAttempt> findByProviderAndRequestReferenceForUpdate(
            @Param("provider") String provider,
            @Param("reference") String reference
    );

    Optional<PaymentAttempt> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);

    Optional<PaymentAttempt> findFirstByOrderIdAndStatusOrderByOpenedAtDesc(
            Long orderId,
            PaymentAttemptStatus status
    );

    Optional<PaymentAttempt> findFirstByOrderIdOrderByOpenedAtDesc(Long orderId);

    @Query("SELECT p.id FROM PaymentAttempt p WHERE p.status = :status AND p.dueAt <= :now ORDER BY p.dueAt ASC")
    List<Long> findDueAttemptIds(
            @Param("status") PaymentAttemptStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentAttempt p WHERE p.id = :id")
    Optional<PaymentAttempt> findByIdForUpdate(@Param("id") Long id);
}
