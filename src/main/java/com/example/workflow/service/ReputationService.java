package com.example.workflow.service;

import com.example.workflow.dto.ReputationHistoryDTO;
import com.example.workflow.cache.CacheKeys;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.entity.ReputationHistory;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.repository.ReputationHistoryRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReputationService {
    private final ReputationHistoryRepository reputationHistoryRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationCacheService applicationCacheService;

    @Transactional
    public User changeReputation(User user, int delta, String reason, String referenceType, String referenceId) {
        int balanceAfter = user.getReputation() + delta;
        if (balanceAfter < 0) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Reputation balance is not enough for this action."
            );
        }

        user.setReputation(balanceAfter);
        User savedUser = userRepository.save(user);

        ReputationHistory history = new ReputationHistory();
        history.setUser(savedUser);
        history.setDelta(delta);
        history.setBalanceAfter(balanceAfter);
        history.setReason(reason);
        history.setReferenceType(referenceType);
        history.setReferenceId(referenceId);
        reputationHistoryRepository.save(history);

        applicationCacheService.evictReputationChanged(savedUser.getId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.REPUTATION_HISTORIES,
            key = "T(com.example.workflow.cache.CacheKeys).reputationHistories(@currentUserService.requireCurrentUserId(), #pageable)",
            unless = "#result == null"
    )
    public Page<ReputationHistoryDTO> getMyHistory(Pageable pageable) {
        String userId = currentUserService.requireCurrentUserId();
        return reputationHistoryRepository.findByUser_IdOrderByCreatedAtDesc(userId, PageableUtils.normalize(pageable, 5, 50))
                .map(this::toDto);
    }

    private ReputationHistoryDTO toDto(ReputationHistory history) {
        return new ReputationHistoryDTO(
                history.getId(),
                history.getDelta(),
                history.getBalanceAfter(),
                history.getReason(),
                history.getReferenceType(),
                history.getReferenceId(),
                history.getCreatedAt()
        );
    }

}
