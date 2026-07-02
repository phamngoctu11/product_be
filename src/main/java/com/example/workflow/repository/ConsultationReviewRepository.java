package com.example.workflow.repository;

import com.example.workflow.entity.ConsultationReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConsultationReviewRepository extends JpaRepository<ConsultationReview, Long> {
    boolean existsByAttributionId(Long attributionId);

    @Query("SELECT r.attribution.id FROM ConsultationReview r WHERE r.attribution.id IN :attributionIds")
    List<Long> findReviewedAttributionIds(@Param("attributionIds") Collection<Long> attributionIds);

    Optional<ConsultationReview> findByAttributionIdAndUserId(Long attributionId, String userId);

    Page<ConsultationReview> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<ConsultationReview> findByStaffIdOrderByCreatedAtDesc(String staffId, Pageable pageable);
}
