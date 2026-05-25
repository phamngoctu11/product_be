package com.example.workflow.service;

import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import com.example.workflow.mapper.VoucherMapper;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.repository.UserVoucherRepository;
import com.example.workflow.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherTemplateRepository templateRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;

    public List<VoucherTemplateDTO> getActiveTemplates() {
        return templateRepository.findAvailableTemplates(LocalDateTime.now())
                .stream()
                .map(voucherMapper::toTemplateDto)
                .collect(Collectors.toList());
    }

    public List<UserVoucherDTO> getMyWallet(Long userId) {
        return userVoucherRepository.findByUserIdAndIsUsedFalse(userId)
                .stream()
                .map(voucherMapper::toUserVoucherDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user", key = "#userId"),
            @CacheEvict(value = "users", allEntries = true)
    })
    public UserVoucherDTO redeemVoucher(Long userId, Long templateId) {
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        VoucherTemplate template = templateRepository.findById(templateId).orElseThrow(() -> new RuntimeException("Voucher does not exist"));

        if (template.getExpiryDate().isBefore(now)) {
            throw new IllegalStateException("Voucher campaign has expired.");
        }

        int remainingPoints = user.getReputation() - template.getPointCost();
        if (remainingPoints < 40) {
            throw new IllegalStateException("Not enough available reputation points.");
        }

        int updatedRows = templateRepository.decrementQuantity(templateId, now);
        if (updatedRows == 0) {
            throw new IllegalStateException("Voucher is out of stock or expired.");
        }

        user.setReputation(remainingPoints);
        userRepository.save(user);

        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setUser(user);
        userVoucher.setTemplate(template);
        userVoucher.setUsed(false);
        userVoucher.setRedeemDate(now);
        userVoucher.setExpiryDate(template.getExpiryDate());

        return voucherMapper.toUserVoucherDto(userVoucherRepository.save(userVoucher));
    }

    public VoucherTemplate createNewVoucherCampaign(VoucherTemplate request) {
        request.setId(null);
        request.setActive(true);
        return templateRepository.save(request);
    }
}
