package com.example.workflow.repository;

import com.example.workflow.entity.VoucherTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherTemplateRepository extends JpaRepository<VoucherTemplate, Long> {
    @Query("SELECT v FROM VoucherTemplate v WHERE v.isActive = true AND v.guestVoucher = false AND v.quantity > 0 AND v.expiryDate > :now")
    List<VoucherTemplate> findAvailableTemplates(@Param("now") LocalDateTime now);

    @Query("SELECT v FROM VoucherTemplate v WHERE v.isActive = true AND v.quantity > 0 AND v.expiryDate > :now")
    List<VoucherTemplate> findAvailableTemplatesForManagement(@Param("now") LocalDateTime now);

    @Query("SELECT v FROM VoucherTemplate v WHERE v.isActive = true AND v.guestVoucher = true AND v.quantity > 0 AND v.expiryDate > :now")
    List<VoucherTemplate> findAvailableGuestTemplates(@Param("now") LocalDateTime now);

    @Query("SELECT v FROM VoucherTemplate v WHERE LOWER(v.code) = LOWER(:code)")
    Optional<VoucherTemplate> findByCodeIgnoreCase(@Param("code") String code);

    @Modifying
    @Query("UPDATE VoucherTemplate v SET v.quantity = v.quantity - 1 WHERE v.id = :id AND v.guestVoucher = false AND v.quantity > 0 AND v.isActive = true AND v.expiryDate > :now")
    int decrementQuantity(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE VoucherTemplate v SET v.quantity = v.quantity - 1 WHERE v.id = :id AND v.guestVoucher = true AND v.quantity > 0 AND v.isActive = true AND v.expiryDate > :now")
    int decrementGuestQuantity(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE VoucherTemplate v SET v.quantity = v.quantity + 1 WHERE v.id = :id AND v.guestVoucher = true")
    int incrementGuestQuantity(@Param("id") Long id);
}
