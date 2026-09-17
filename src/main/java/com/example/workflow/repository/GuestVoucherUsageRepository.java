package com.example.workflow.repository;

import com.example.workflow.entity.GuestVoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestVoucherUsageRepository extends JpaRepository<GuestVoucherUsage, Long> {
    boolean existsByVoucherTemplateIdAndGuestSessionId(Long voucherTemplateId, String guestSessionId);

    boolean existsByVoucherTemplateIdAndEmailHash(Long voucherTemplateId, String emailHash);

    boolean existsByVoucherTemplateIdAndPhoneHash(Long voucherTemplateId, String phoneHash);
}
