package com.example.workflow.repository;
import com.example.workflow.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findByUserIdAndIsUsedFalse(String userId);

    @Query("SELECT uv FROM UserVoucher uv " +
            "JOIN FETCH uv.template t " +
            "WHERE uv.user.id = :userId " +
            "AND uv.isUsed = false " +
            "AND uv.expiryDate > :now " +
            "AND t.isActive = true")
    List<UserVoucher> findAvailableWallet(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(uv) > 0 FROM UserVoucher uv " +
            "JOIN uv.template t " +
            "WHERE uv.user.id = :userId " +
            "AND t.id = :templateId " +
            "AND uv.isUsed = false " +
            "AND uv.expiryDate > :now " +
            "AND t.isActive = true")
    boolean existsAvailableByUserIdAndTemplateId(
            @Param("userId") String userId,
            @Param("templateId") Long templateId,
            @Param("now") LocalDateTime now
    );
}
