package com.example.workflow.repository;

import com.example.workflow.entity.CustomRequest;
import com.example.workflow.nume.CustomRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomRequestRepository extends JpaRepository<CustomRequest, Long> {
    Page<CustomRequest> findAllByOwnerIdOrderByUpdatedAtDescIdDesc(
            String ownerId,
            Pageable pageable
    );

    Page<CustomRequest> findAllByOwnerIdAndStatusOrderByUpdatedAtDescIdDesc(
            String ownerId,
            CustomRequestStatus status,
            Pageable pageable
    );

    Optional<CustomRequest> findByIdAndOwnerId(Long id, String ownerId);

    Optional<CustomRequest> findByIdAndOwnerIdAndStatus(
            Long id,
            String ownerId,
            CustomRequestStatus status
    );

    Optional<CustomRequest> findByLinkedOrderId(Long linkedOrderId);

    boolean existsByIdAndOwnerId(Long id, String ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from CustomRequest request " +
            "where request.id = :id and request.ownerId = :ownerId")
    Optional<CustomRequest> findOwnedByIdForUpdate(
            @Param("id") Long id,
            @Param("ownerId") String ownerId
    );
}
