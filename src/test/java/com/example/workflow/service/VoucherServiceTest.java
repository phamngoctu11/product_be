package com.example.workflow.service;

import com.example.workflow.entity.User;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.mapper.VoucherMapper;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.repository.VoucherTemplateRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherServiceTest {
    private final VoucherTemplateRepository templateRepository = mock(VoucherTemplateRepository.class);
    private final UserVoucherRepository userVoucherRepository = mock(UserVoucherRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final VoucherMapper voucherMapper = mock(VoucherMapper.class);
    private final VoucherService voucherService = new VoucherService(
            templateRepository,
            userVoucherRepository,
            userRepository,
            voucherMapper
    );

    @Test
    void doesNotDecrementCampaignQuantityWhenUserHasInsufficientPoints() {
        User user = new User();
        user.setReputation(50);
        VoucherTemplate template = new VoucherTemplate();
        template.setPointCost(20);
        template.setExpiryDate(LocalDateTime.now().plusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(templateRepository.findById(2L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> voucherService.redeemVoucher(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not enough available reputation points.");

        verify(templateRepository, never()).decrementQuantity(eq(2L), any(LocalDateTime.class));
    }
}
