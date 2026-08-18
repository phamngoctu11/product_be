package com.example.workflow.service;

import com.example.workflow.dto.CartVoucherOptionsDTO;
import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherCartOptionDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.VoucherMapper;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherTemplateRepository templateRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;
    private final AuthService authService;
    private final ReputationService reputationService;

    @Transactional(readOnly = true)
    @Cacheable(value = "voucherTemplates", key = "'active'", unless = "#result == null")
    public List<VoucherTemplateDTO> getActiveTemplates() {
        return templateRepository.findAvailableTemplates(LocalDateTime.now())
                .stream()
                .map(voucherMapper::toTemplateDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "voucherTemplates", key = "'management-active'", unless = "#result == null")
    public List<VoucherTemplateDTO> getActiveTemplatesForManagement() {
        return templateRepository.findAvailableTemplatesForManagement(LocalDateTime.now())
                .stream()
                .map(voucherMapper::toTemplateDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "guestVoucherTemplates", key = "'active-' + #subtotal", unless = "#result == null")
    public List<VoucherCartOptionDTO> getGuestVoucherOptions(double subtotal) {
        double safeSubtotal = Math.max(0, subtotal);
        return templateRepository.findAvailableGuestTemplates(LocalDateTime.now())
                .stream()
                .map(template -> buildCartOption(null, template, "GUEST", safeSubtotal))
                .sorted(cartOptionComparator())
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "userVoucherWallet",
            key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()",
            unless = "#result == null"
    )
    public List<UserVoucherDTO> getMyWallet() {
        String userId = authService.getCurrentUserId();
        return userVoucherRepository.findByUserIdAndIsUsedFalse(userId)
                .stream()
                .map(voucherMapper::toUserVoucherDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CartVoucherOptionsDTO getCartVoucherOptions(double subtotal) {
        String userId = authService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        User user = getUserOrThrow(userId);
        double safeSubtotal = Math.max(0, subtotal);
        int redeemableReputation = Math.max(0, user.getReputation() - 40);

        List<VoucherCartOptionDTO> walletOptions = userVoucherRepository.findAvailableWallet(userId, now)
                .stream()
                .map(userVoucher -> buildCartOption(
                        userVoucher.getId(),
                        userVoucher.getTemplate(),
                        "WALLET",
                        safeSubtotal
                ))
                .sorted(cartOptionComparator())
                .toList();

        Set<Long> ownedTemplateIds = walletOptions.stream()
                .map(VoucherCartOptionDTO::getTemplateId)
                .collect(Collectors.toSet());

        List<VoucherCartOptionDTO> redeemableOptions = templateRepository.findAvailableTemplates(now)
                .stream()
                .filter(template -> !ownedTemplateIds.contains(template.getId()))
                .filter(template -> template.getPointCost() <= redeemableReputation)
                .map(template -> buildCartOption(null, template, "REDEEMABLE", safeSubtotal))
                .sorted(cartOptionComparator())
                .toList();

        VoucherCartOptionDTO bestWalletVoucher = markBestOption(walletOptions);
        VoucherCartOptionDTO bestRedeemableVoucher = markBestOption(redeemableOptions);

        return new CartVoucherOptionsDTO(
                safeSubtotal,
                user.getReputation(),
                redeemableReputation,
                bestWalletVoucher,
                bestRedeemableVoucher,
                walletOptions,
                redeemableOptions
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "voucherTemplates", allEntries = true),
            @CacheEvict(value = "userVoucherWallet", allEntries = true)
    })
    public UserVoucherDTO redeemVoucher(Long templateId) {
        String userId = authService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        User user = getUserOrThrow(userId);
        VoucherTemplate template = getTemplateOrThrow(templateId);

        validateTemplateForRedeem(template, now);
        validateTemplateNotAlreadyOwned(userId, templateId, now);
        int remainingPoints = user.getReputation() - template.getPointCost();
        validateRemainingPoints(remainingPoints);

        int updatedRows = templateRepository.decrementQuantity(templateId, now);
        if (updatedRows == 0) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Voucher is out of stock or expired."
            );
        }

        reputationService.changeReputation(
                user,
                -template.getPointCost(),
                "Redeemed voucher #" + templateId,
                "VOUCHER_TEMPLATE",
                String.valueOf(templateId)
        );

        UserVoucher userVoucher = createUserVoucher(user, template, now);
        return voucherMapper.toUserVoucherDto(userVoucherRepository.save(userVoucher));
    }

    @Transactional
    @CacheEvict(value = "userVoucherWallet", allEntries = true, condition = "#userVoucherId != null")
    public UserVoucher useVoucherForCheckout(Long userVoucherId, String userId, double totalPrice) {
        if (userVoucherId == null) {
            return null;
        }

        UserVoucher voucher = userVoucherRepository.findById(userVoucherId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VOUCHER_NOT_FOUND));
        validateVoucherForCheckout(voucher, userId, totalPrice);
        voucher.setUsed(true);
        voucher.setUsedDate(LocalDateTime.now());
        return userVoucherRepository.save(voucher);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "guestVoucherTemplates", allEntries = true, condition = "T(org.springframework.util.StringUtils).hasText(#voucherCode)"),
            @CacheEvict(value = "voucherTemplates", allEntries = true, condition = "T(org.springframework.util.StringUtils).hasText(#voucherCode)")
    })
    public AppliedGuestVoucher applyGuestVoucherForCheckout(String voucherCode, double totalPrice) {
        if (!org.springframework.util.StringUtils.hasText(voucherCode)) {
            return AppliedGuestVoucher.none();
        }

        String normalizedCode = voucherCode.trim();
        LocalDateTime now = LocalDateTime.now();
        VoucherTemplate template = templateRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VOUCHER_NOT_FOUND));
        validateGuestVoucherForCheckout(template, totalPrice, now);

        double discountAmount = calculateDiscountAmount(template, totalPrice);
        int updatedRows = templateRepository.decrementGuestQuantity(template.getId(), now);
        if (updatedRows == 0) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Voucher is out of stock or expired."
            );
        }
        return new AppliedGuestVoucher(template, discountAmount);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "guestVoucherTemplates", allEntries = true, condition = "#template != null"),
            @CacheEvict(value = "voucherTemplates", allEntries = true, condition = "#template != null")
    })
    public void restoreGuestVoucher(VoucherTemplate template) {
        if (template == null || template.getId() == null || !template.isGuestVoucher()) {
            return;
        }
        templateRepository.incrementGuestQuantity(template.getId());
    }

    public double calculateDiscountAmount(UserVoucher voucher, double totalPrice) {
        if (voucher == null) {
            return 0.0;
        }
        return calculateDiscountAmount(voucher.getTemplate(), totalPrice);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "voucherTemplates", allEntries = true),
            @CacheEvict(value = "guestVoucherTemplates", allEntries = true)
    })
    public VoucherTemplate createNewVoucherCampaign(VoucherTemplate request) {
        request.setId(null);
        request.setActive(true);
        return templateRepository.save(request);
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
    }

    private VoucherTemplate getTemplateOrThrow(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.VOUCHER_NOT_FOUND));
    }

    private void validateTemplateForRedeem(VoucherTemplate template, LocalDateTime now) {
        if (template.isGuestVoucher()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_INVALID);
        }
        if (!template.isActive() || template.getQuantity() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Voucher is not available.");
        }
        if (template.getExpiryDate().isBefore(now)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_EXPIRED);
        }
    }

    private void validateRemainingPoints(int remainingPoints) {
        if (remainingPoints < 40) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Not enough available reputation points."
            );
        }
    }

    private void validateTemplateNotAlreadyOwned(String userId, Long templateId, LocalDateTime now) {
        if (userVoucherRepository.existsAvailableByUserIdAndTemplateId(userId, templateId, now)) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "You already have this voucher in your wallet."
            );
        }
    }

    private UserVoucher createUserVoucher(User user, VoucherTemplate template, LocalDateTime redeemTime) {
        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setUser(user);
        userVoucher.setTemplate(template);
        userVoucher.setUsed(false);
        userVoucher.setRedeemDate(redeemTime);
        userVoucher.setExpiryDate(template.getExpiryDate());
        return userVoucher;
    }

    private VoucherCartOptionDTO buildCartOption(Long userVoucherId, VoucherTemplate template, String source, double subtotal) {
        boolean applicable = subtotal >= template.getMinOrderValue();
        double discountAmount = applicable ? calculateDiscountAmount(template, subtotal) : 0.0;
        String unavailableReason = applicable
                ? null
                : "Can them " + Math.ceil(template.getMinOrderValue() - subtotal) + "d de ap dung voucher nay.";

        return new VoucherCartOptionDTO(
                userVoucherId,
                template.getId(),
                source,
                voucherMapper.toTemplateDto(template),
                applicable,
                false,
                discountAmount,
                Math.max(0, subtotal - discountAmount),
                unavailableReason
        );
    }

    private VoucherCartOptionDTO markBestOption(List<VoucherCartOptionDTO> options) {
        VoucherCartOptionDTO bestOption = options.stream()
                .filter(VoucherCartOptionDTO::isApplicable)
                .max(Comparator
                        .comparingDouble(VoucherCartOptionDTO::getDiscountAmount)
                        .thenComparing(option -> option.getTemplate().getMinOrderValue(), Comparator.reverseOrder()))
                .orElse(null);
        if (bestOption != null) {
            bestOption.setBest(true);
        }
        return bestOption;
    }

    private Comparator<VoucherCartOptionDTO> cartOptionComparator() {
        return Comparator
                .comparing(VoucherCartOptionDTO::isApplicable, Comparator.reverseOrder())
                .thenComparing(VoucherCartOptionDTO::getDiscountAmount, Comparator.reverseOrder())
                .thenComparing(option -> option.getTemplate().getMinOrderValue())
                .thenComparing(option -> option.getTemplate().getExpiryDate());
    }

    private void validateVoucherForCheckout(UserVoucher voucher, String userId, double totalPrice) {
        if (voucher.isUsed()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_ALREADY_USED);
        }
        if (voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_EXPIRED);
        }
        if (!voucher.getUser().getId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.VOUCHER_INVALID);
        }
        if (totalPrice < voucher.getTemplate().getMinOrderValue()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_MINIMUM_NOT_MET);
        }
    }

    private void validateGuestVoucherForCheckout(VoucherTemplate template, double totalPrice, LocalDateTime now) {
        if (!template.isGuestVoucher()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_INVALID);
        }
        if (!template.isActive() || template.getQuantity() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Voucher is not available.");
        }
        if (template.getExpiryDate().isBefore(now)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.VOUCHER_EXPIRED);
        }
        if (totalPrice < template.getMinOrderValue()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ORDER_MINIMUM_NOT_MET);
        }
    }

    private double calculateDiscountAmount(VoucherTemplate template, double totalPrice) {
        if (template.getDiscountPercent() <= 0) {
            return Math.min(totalPrice, template.getMaxDiscountAmount());
        }

        double discountAmount = (totalPrice * template.getDiscountPercent()) / 100;
        if (template.getMaxDiscountAmount() > 0 && discountAmount > template.getMaxDiscountAmount()) {
            return template.getMaxDiscountAmount();
        }
        return Math.min(totalPrice, discountAmount);
    }

    public record AppliedGuestVoucher(VoucherTemplate template, double discountAmount) {
        public static AppliedGuestVoucher none() {
            return new AppliedGuestVoucher(null, 0.0);
        }

        public boolean applied() {
            return template != null && discountAmount > 0;
        }
    }
}
