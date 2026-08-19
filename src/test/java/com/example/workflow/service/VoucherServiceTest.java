package com.example.workflow.service;

import com.example.workflow.entity.User;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.VoucherMapper;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.repository.VoucherTemplateRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {
    private final VoucherTemplateRepository templateRepository = mock(VoucherTemplateRepository.class);
    private final UserVoucherRepository userVoucherRepository = mock(UserVoucherRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final VoucherMapper voucherMapper = mock(VoucherMapper.class);
    private final AuthService authService = mock(AuthService.class);
    private final ReputationService reputationService = mock(ReputationService.class);
    private final ApplicationCacheService applicationCacheService = mock(ApplicationCacheService.class);
    private final VoucherService voucherService = new VoucherService(
            templateRepository,
            userVoucherRepository,
            userRepository,
            voucherMapper,
            authService,
            reputationService,
            applicationCacheService
    );

    @Test
    void applyGuestVoucherDecrementsQuantityAtomicallyAndReturnsDiscount() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));
        when(templateRepository.decrementGuestQuantity(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(1);

        VoucherService.AppliedGuestVoucher result = voucherService.applyGuestVoucherForCheckout("WELCOME10", 500.0);

        assertThat(result.template()).isSameAs(template);
        assertThat(result.discountAmount()).isEqualTo(30.0);
        verify(templateRepository).decrementGuestQuantity(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void applyGuestVoucherFailsWhenAtomicDecrementLosesRace() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));
        when(templateRepository.decrementGuestQuantity(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> voucherService.applyGuestVoucherForCheckout("WELCOME10", 500.0))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void redeemVoucherRejectsGuestVoucher() {
        User user = new User();
        user.setId("user-1");
        user.setReputation(100);
        when(authService.getCurrentUserId()).thenReturn("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(templateRepository.findById(7L)).thenReturn(Optional.of(guestVoucher()));

        assertThatThrownBy(() -> voucherService.redeemVoucher(7L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(templateRepository, never()).decrementQuantity(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    private VoucherTemplate guestVoucher() {
        VoucherTemplate template = new VoucherTemplate();
        template.setId(7L);
        template.setCode("WELCOME10");
        template.setName("Welcome guest");
        template.setGuestVoucher(true);
        template.setActive(true);
        template.setQuantity(1);
        template.setMinOrderValue(300.0);
        template.setDiscountPercent(10.0);
        template.setMaxDiscountAmount(30.0);
        template.setExpiryDate(LocalDateTime.now().plusDays(1));
        return template;
    }
}
