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
    @Query(value = "select request from CustomRequest request " +
            "where request.ownerId = :ownerId and request.isDelete = false " +
            "order by request.updatedAt desc, request.id desc",
            countQuery = "select count(request) from CustomRequest request " +
                    "where request.ownerId = :ownerId and request.isDelete = false")
    Page<CustomRequest> findAllByOwnerIdOrderByUpdatedAtDescIdDesc(
            @Param("ownerId") String ownerId,
            Pageable pageable
    );

    @Query(value = "select request from CustomRequest request " +
            "where request.ownerId = :ownerId and request.status = :status and request.isDelete = false " +
            "order by request.updatedAt desc, request.id desc",
            countQuery = "select count(request) from CustomRequest request " +
                    "where request.ownerId = :ownerId and request.status = :status and request.isDelete = false")
    Page<CustomRequest> findAllByOwnerIdAndStatusOrderByUpdatedAtDescIdDesc(
            @Param("ownerId") String ownerId,
            @Param("status") CustomRequestStatus status,
            Pageable pageable
    );

    @Query("select request from CustomRequest request " +
            "where request.id = :id and request.ownerId = :ownerId and request.isDelete = false")
    Optional<CustomRequest> findByIdAndOwnerId(
            @Param("id") Long id,
            @Param("ownerId") String ownerId
    );

    @Query("select request from CustomRequest request " +
            "where request.id = :id and request.ownerId = :ownerId " +
            "and request.status = :status and request.isDelete = false")
    Optional<CustomRequest> findByIdAndOwnerIdAndStatus(
            @Param("id") Long id,
            @Param("ownerId") String ownerId,
            @Param("status") CustomRequestStatus status
    );

    @Query("select request from CustomRequest request " +
            "where request.linkedOrderId = :linkedOrderId and request.isDelete = false")
    Optional<CustomRequest> findByLinkedOrderId(@Param("linkedOrderId") Long linkedOrderId);

    @Query("select case when count(request) > 0 then true else false end from CustomRequest request " +
            "where request.id = :id and request.ownerId = :ownerId and request.isDelete = false")
    boolean existsByIdAndOwnerId(
            @Param("id") Long id,
            @Param("ownerId") String ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from CustomRequest request " +
            "where request.id = :id and request.ownerId = :ownerId and request.isDelete = false")
    Optional<CustomRequest> findOwnedByIdForUpdate(
            @Param("id") Long id,
            @Param("ownerId") String ownerId
    );
}
