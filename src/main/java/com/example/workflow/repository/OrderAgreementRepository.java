package com.example.workflow.repository;

import com.example.workflow.entity.OrderAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface OrderAgreementRepository extends JpaRepository<OrderAgreement, Long> {
    Optional<OrderAgreement> findFirstByOrderIdOrderByAgreementVersionDesc(Long orderId);

    Optional<OrderAgreement> findByOrderIdAndAgreementVersion(Long orderId, Integer agreementVersion);

    boolean existsByOrderIdAndSubmittedAtIsNotNull(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from OrderAgreement a where a.orderId = :orderId and a.agreementVersion = :agreementVersion")
    Optional<OrderAgreement> findByOrderIdAndAgreementVersionForUpdate(
            @Param("orderId") Long orderId,
            @Param("agreementVersion") Integer agreementVersion
    );
}
