package com.example.workflow.service;

import com.example.workflow.entity.User;
import com.example.workflow.entity.GuestVoucherUsage;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.VoucherMapper;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.repository.VoucherTemplateRepository;
import com.example.workflow.repository.GuestVoucherUsageRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

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
    private final GuestVoucherUsageRepository guestVoucherUsageRepository = mock(GuestVoucherUsageRepository.class);
    private final VoucherService voucherService = new VoucherService(
            templateRepository,
            userVoucherRepository,
            userRepository,
            voucherMapper,
            authService,
            reputationService,
            applicationCacheService,
            guestVoucherUsageRepository
    );

    @Test
    void applyGuestVoucherDecrementsQuantityAtomicallyAndReturnsDiscount() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));
        when(templateRepository.decrementGuestQuantity(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(1);

        VoucherService.AppliedGuestVoucher result = voucherService.applyGuestVoucherForCheckout(
                "WELCOME10", 500.0, "guest-session-0001", " Guest@Example.com ", "+84 900-000-000");

        assertThat(result.template()).isSameAs(template);
        assertThat(result.discountAmount()).isEqualTo(30.0);
        assertThat(result.guestSessionId()).isEqualTo("guest-session-0001");
        assertThat(result.emailHash()).hasSize(64);
        assertThat(result.phoneHash()).hasSize(64);
        verify(templateRepository).decrementGuestQuantity(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void applyGuestVoucherFailsWhenAtomicDecrementLosesRace() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));
        when(templateRepository.decrementGuestQuantity(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> voucherService.applyGuestVoucherForCheckout(
                "WELCOME10", 500.0, "guest-session-0001", "guest@example.com", "0900000000"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void guestVoucherOptionsMarkVoucherUsedByCurrentSessionAsUnavailable() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findAvailableGuestTemplates(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(template));
        when(guestVoucherUsageRepository.existsByVoucherTemplateIdAndGuestSessionId(7L, "guest-session-0001"))
                .thenReturn(true);

        var options = voucherService.getGuestVoucherOptions(500.0, "guest-session-0001");

        assertThat(options).singleElement().satisfies(option -> {
            assertThat(option.isApplicable()).isFalse();
            assertThat(option.getDiscountAmount()).isZero();
            assertThat(option.getFinalPrice()).isEqualTo(500.0);
            assertThat(option.getUnavailableReason()).contains("phiên khách");
        });
    }

    @Test
    void applyGuestVoucherRejectsExpiredCampaign() {
        VoucherTemplate template = guestVoucher();
        template.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> voucherService.applyGuestVoucherForCheckout(
                "WELCOME10", 500.0, "guest-session-0001", "guest@example.com", "0900000000"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(templateRepository, never()).decrementGuestQuantity(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void applyGuestVoucherRejectsOrderBelowCampaignMinimum() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> voucherService.applyGuestVoucherForCheckout(
                "WELCOME10", 299.0, "guest-session-0001", "guest@example.com", "0900000000"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(templateRepository, never()).decrementGuestQuantity(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void applyGuestVoucherRejectsReusedEmailBeforeDecrementingQuantity() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));
        when(guestVoucherUsageRepository.existsByVoucherTemplateIdAndEmailHash(
                org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);

        assertThatThrownBy(() -> voucherService.applyGuestVoucherForCheckout(
                "WELCOME10", 500.0, "guest-session-0002", "GUEST@example.com", "0911111111"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("already been used");

        verify(templateRepository, never()).decrementGuestQuantity(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void applyGuestVoucherRejectsReusedPhoneBeforeDecrementingQuantity() {
        VoucherTemplate template = guestVoucher();
        when(templateRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(template));
        when(guestVoucherUsageRepository.existsByVoucherTemplateIdAndPhoneHash(
                org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);

        assertThatThrownBy(() -> voucherService.applyGuestVoucherForCheckout(
                "WELCOME10", 500.0, "guest-session-0003", "another@example.com", "+84 900 000 000"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(templateRepository, never()).decrementGuestQuantity(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void userVoucherCheckoutRemainsIndependentFromGuestUsage() {
        User user = new User();
        user.setId("user-1");
        VoucherTemplate template = guestVoucher();
        template.setGuestVoucher(false);
        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setId(12L);
        userVoucher.setUser(user);
        userVoucher.setTemplate(template);
        userVoucher.setExpiryDate(LocalDateTime.now().plusDays(1));
        when(userVoucherRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(userVoucher));
        when(userVoucherRepository.save(userVoucher)).thenReturn(userVoucher);

        UserVoucher result = voucherService.useVoucherForCheckout(12L, "user-1", 500.0);

        assertThat(result.isUsed()).isTrue();
        assertThat(result.getUsedDate()).isNotNull();
        verify(userVoucherRepository).save(userVoucher);
        verify(guestVoucherUsageRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordGuestVoucherUsagePersistsOnlyHashesAndOrderReference() {
        VoucherTemplate template = guestVoucher();
        Order order = new Order();
        order.setId(99L);
        VoucherService.AppliedGuestVoucher applied = new VoucherService.AppliedGuestVoucher(
                template, 30.0, "guest-session-0001", "a".repeat(64), "b".repeat(64));

        voucherService.recordGuestVoucherUsage(applied, order);

        ArgumentCaptor<GuestVoucherUsage> captor = ArgumentCaptor.forClass(GuestVoucherUsage.class);
        verify(guestVoucherUsageRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue()).satisfies(usage -> {
            assertThat(usage.getVoucherTemplate()).isSameAs(template);
            assertThat(usage.getOrder()).isSameAs(order);
            assertThat(usage.getGuestSessionId()).isEqualTo("guest-session-0001");
            assertThat(usage.getEmailHash()).isEqualTo("a".repeat(64));
            assertThat(usage.getPhoneHash()).isEqualTo("b".repeat(64));
            assertThat(usage.getUsedAt()).isNotNull();
        });
    }

    @Test
    void recordGuestVoucherUsageTranslatesConcurrentUniqueConstraintViolation() {
        VoucherService.AppliedGuestVoucher applied = new VoucherService.AppliedGuestVoucher(
                guestVoucher(), 30.0, "guest-session-0001", "a".repeat(64), "b".repeat(64));
        when(guestVoucherUsageRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> voucherService.recordGuestVoucherUsage(applied, new Order()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
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
