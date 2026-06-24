package com.example.workflow.repository;

import com.example.workflow.dto.ConsultationRequestDTO;
import com.example.workflow.entity.ConsultationRequest;
import com.example.workflow.nume.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {
    Optional<ConsultationRequest> findFirstByUserIdAndProductIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            Long productId,
            Collection<ConsultationStatus> statuses
    );

    boolean existsByUserIdAndAssignedStaffIdAndStatusIn(
            Long userId,
            Long assignedStaffId,
            Collection<ConsultationStatus> statuses
    );

    Optional<ConsultationRequest> findFirstByUserIdAndAssignedStaffIdAndStatusInOrderByLastMessageAtDescCreatedAtDesc(
            Long userId,
            Long assignedStaffId,
            Collection<ConsultationStatus> statuses
    );

    Optional<ConsultationRequest> findFirstByUserIdAndProductIdAndAssignedStaffIsNotNullAndStatusInAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId,
            Long productId,
            Collection<ConsultationStatus> statuses,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<ConsultationRequest> findFirstByUserIdAndProductIdAndAssignedStaffIsNotNullAndFirstStaffReplyAtIsNotNullAndStatusInAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId,
            Long productId,
            Collection<ConsultationStatus> statuses,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("SELECT c FROM ConsultationRequest c " +
            "WHERE c.user.id = :userId " +
            "AND c.product.id IN :productIds " +
            "AND c.assignedStaff IS NOT NULL " +
            "AND c.firstStaffReplyAt IS NOT NULL " +
            "AND c.status IN :statuses " +
            "AND c.createdAt BETWEEN :from AND :to " +
            "ORDER BY c.product.id ASC, c.createdAt DESC")
    List<ConsultationRequest> findAttributionCandidates(
            @Param("userId") Long userId,
            @Param("productIds") Collection<Long> productIds,
            @Param("statuses") Collection<ConsultationStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT new com.example.workflow.dto.ConsultationRequestDTO(" +
            "c.id, u.id, CONCAT(CONCAT(u.lastname, ' '), u.firstname), " +
            "p.id, p.productName, p.imageUrl, c.status, " +
            "staff.id, CASE WHEN staff.id IS NULL THEN null ELSE CONCAT(CONCAT(staff.lastname, ' '), staff.firstname) END, " +
            "manager.id, CASE WHEN manager.id IS NULL THEN null ELSE CONCAT(CONCAT(manager.lastname, ' '), manager.firstname) END, " +
            "c.createdAt, c.assignedAt, c.claimedAt, c.lastMessageAt) " +
            "FROM ConsultationRequest c " +
            "JOIN c.user u " +
            "JOIN c.product p " +
            "LEFT JOIN c.assignedStaff staff " +
            "LEFT JOIN c.assignedByManager manager " +
            "WHERE c.id = :id")
    Optional<ConsultationRequestDTO> findDtoById(@Param("id") Long id);

    @Query("SELECT new com.example.workflow.dto.ConsultationRequestDTO(" +
            "c.id, u.id, CONCAT(CONCAT(u.lastname, ' '), u.firstname), " +
            "p.id, p.productName, p.imageUrl, c.status, " +
            "staff.id, CASE WHEN staff.id IS NULL THEN null ELSE CONCAT(CONCAT(staff.lastname, ' '), staff.firstname) END, " +
            "manager.id, CASE WHEN manager.id IS NULL THEN null ELSE CONCAT(CONCAT(manager.lastname, ' '), manager.firstname) END, " +
            "c.createdAt, c.assignedAt, c.claimedAt, c.lastMessageAt) " +
            "FROM ConsultationRequest c " +
            "JOIN c.user u " +
            "JOIN c.product p " +
            "LEFT JOIN c.assignedStaff staff " +
            "LEFT JOIN c.assignedByManager manager " +
            "WHERE c.status = :status " +
            "ORDER BY c.createdAt ASC")
    Page<ConsultationRequestDTO> findDtosByStatus(
            @Param("status") ConsultationStatus status,
            Pageable pageable
    );

    @Query("SELECT new com.example.workflow.dto.ConsultationRequestDTO(" +
            "c.id, u.id, CONCAT(CONCAT(u.lastname, ' '), u.firstname), " +
            "p.id, p.productName, p.imageUrl, c.status, " +
            "staff.id, CASE WHEN staff.id IS NULL THEN null ELSE CONCAT(CONCAT(staff.lastname, ' '), staff.firstname) END, " +
            "manager.id, CASE WHEN manager.id IS NULL THEN null ELSE CONCAT(CONCAT(manager.lastname, ' '), manager.firstname) END, " +
            "c.createdAt, c.assignedAt, c.claimedAt, c.lastMessageAt) " +
            "FROM ConsultationRequest c " +
            "JOIN c.user u " +
            "JOIN c.product p " +
            "LEFT JOIN c.assignedStaff staff " +
            "LEFT JOIN c.assignedByManager manager " +
            "WHERE c.status IN :statuses " +
            "ORDER BY c.lastMessageAt DESC, c.createdAt DESC")
    Page<ConsultationRequestDTO> findDtosByStatusIn(
            @Param("statuses") Collection<ConsultationStatus> statuses,
            Pageable pageable
    );

    @Query("SELECT new com.example.workflow.dto.ConsultationRequestDTO(" +
            "c.id, u.id, CONCAT(CONCAT(u.lastname, ' '), u.firstname), " +
            "p.id, p.productName, p.imageUrl, c.status, " +
            "staff.id, CASE WHEN staff.id IS NULL THEN null ELSE CONCAT(CONCAT(staff.lastname, ' '), staff.firstname) END, " +
            "manager.id, CASE WHEN manager.id IS NULL THEN null ELSE CONCAT(CONCAT(manager.lastname, ' '), manager.firstname) END, " +
            "c.createdAt, c.assignedAt, c.claimedAt, c.lastMessageAt) " +
            "FROM ConsultationRequest c " +
            "JOIN c.user u " +
            "JOIN c.product p " +
            "LEFT JOIN c.assignedStaff staff " +
            "LEFT JOIN c.assignedByManager manager " +
            "WHERE u.id = :userId AND c.status IN :statuses " +
            "ORDER BY c.lastMessageAt DESC, c.createdAt DESC")
    Page<ConsultationRequestDTO> findDtosByUserIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") Collection<ConsultationStatus> statuses,
            Pageable pageable
    );

    @Query("SELECT new com.example.workflow.dto.ConsultationRequestDTO(" +
            "c.id, u.id, CONCAT(CONCAT(u.lastname, ' '), u.firstname), " +
            "p.id, p.productName, p.imageUrl, c.status, " +
            "staff.id, CASE WHEN staff.id IS NULL THEN null ELSE CONCAT(CONCAT(staff.lastname, ' '), staff.firstname) END, " +
            "manager.id, CASE WHEN manager.id IS NULL THEN null ELSE CONCAT(CONCAT(manager.lastname, ' '), manager.firstname) END, " +
            "c.createdAt, c.assignedAt, c.claimedAt, c.lastMessageAt) " +
            "FROM ConsultationRequest c " +
            "JOIN c.user u " +
            "JOIN c.product p " +
            "LEFT JOIN c.assignedStaff staff " +
            "LEFT JOIN c.assignedByManager manager " +
            "WHERE staff.id = :staffId AND c.status IN :statuses " +
            "ORDER BY c.lastMessageAt DESC, c.createdAt DESC")
    Page<ConsultationRequestDTO> findDtosByAssignedStaffIdAndStatusIn(
            @Param("staffId") Long staffId,
            @Param("statuses") Collection<ConsultationStatus> statuses,
            Pageable pageable
    );

    @Modifying
    @Query(value = "UPDATE consultation_requests " +
            "SET assigned_staff_id = :staffId, status = :status, claimed_at = :now, assigned_at = :now " +
            "WHERE id = :id AND status = :waitingStatus AND assigned_staff_id IS NULL",
            nativeQuery = true)
    int claimWaitingRequest(
            @Param("id") Long id,
            @Param("staffId") Long staffId,
            @Param("status") String status,
            @Param("waitingStatus") String waitingStatus,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query(value = "UPDATE consultation_requests " +
            "SET assigned_staff_id = :staffId, assigned_by_manager_id = :managerId, status = :status, assigned_at = :now " +
            "WHERE id = :id AND status = :waitingStatus AND assigned_staff_id IS NULL",
            nativeQuery = true)
    int assignWaitingRequest(
            @Param("id") Long id,
            @Param("staffId") Long staffId,
            @Param("managerId") Long managerId,
            @Param("status") String status,
            @Param("waitingStatus") String waitingStatus,
            @Param("now") LocalDateTime now
    );
}
