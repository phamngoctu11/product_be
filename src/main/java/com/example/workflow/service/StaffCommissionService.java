package com.example.workflow.service;

import com.example.workflow.dto.StaffCommissionDetailDTO;
import com.example.workflow.dto.StaffCommissionSummaryDTO;
import com.example.workflow.entity.ConsultationRequest;
import com.example.workflow.entity.ConsultationSaleAttribution;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.StaffCommissionDailySummary;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.CommissionPeriod;
import com.example.workflow.nume.ConsultationAttributionStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ConsultationReviewRepository;
import com.example.workflow.repository.ConsultationSaleAttributionRepository;
import com.example.workflow.repository.StaffCommissionDailySummaryRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StaffCommissionService {
    private static final double MONEY_ROUNDING_FACTOR = 100.0;
    private static final long MAX_REPORT_DAYS = 370;

    private final ConsultationSaleAttributionRepository attributionRepository;
    private final ConsultationReviewRepository reviewRepository;
    private final StaffCommissionDailySummaryRepository summaryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionSummaries",
            key = "'me-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #period.name() + '-' + #from + '-' + #to"
    )
    public StaffCommissionSummaryDTO getMySummary(CommissionPeriod period, LocalDate from, LocalDate to) {
        User staff = requireStaff(getCurrentUser());
        DateRange range = resolveDateRange(period, from, to);
        return buildStaffSummary(staff, range);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionDetails",
            key = "'me-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #period.name() + '-' + #from + '-' + #to + '-' + #status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public Page<StaffCommissionDetailDTO> getMyDetails(
            CommissionPeriod period,
            LocalDate from,
            LocalDate to,
            ConsultationAttributionStatus status,
            Pageable pageable
    ) {
        User staff = requireStaff(getCurrentUser());
        DateRange range = resolveDateRange(period, from, to);
        return toDetailPage(findDetailPage(staff.getId(), status, range, pageable));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionSummaries",
            key = "'list-' + #period.name() + '-' + #from + '-' + #to + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public Page<StaffCommissionSummaryDTO> getStaffSummaries(
            CommissionPeriod period,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        requireManagerOrAdmin(getCurrentUser());
        DateRange range = resolveDateRange(period, from, to);
        return userRepository.findByRoleAndIsDeleteFalse(Role.STAFF, pageable)
                .map(staff -> buildStaffSummary(staff, range));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionSummaries",
            key = "'staff-' + #staffId + '-' + #period.name() + '-' + #from + '-' + #to"
    )
    public StaffCommissionSummaryDTO getStaffSummary(Long staffId, CommissionPeriod period, LocalDate from, LocalDate to) {
        requireManagerOrAdmin(getCurrentUser());
        User staff = getStaffOrThrow(staffId);
        DateRange range = resolveDateRange(period, from, to);
        return buildStaffSummary(staff, range);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionDetails",
            key = "'staff-' + #staffId + '-' + #period.name() + '-' + #from + '-' + #to + '-' + #status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public Page<StaffCommissionDetailDTO> getStaffDetails(
            Long staffId,
            CommissionPeriod period,
            LocalDate from,
            LocalDate to,
            ConsultationAttributionStatus status,
            Pageable pageable
    ) {
        requireManagerOrAdmin(getCurrentUser());
        getStaffOrThrow(staffId);
        DateRange range = resolveDateRange(period, from, to);
        return toDetailPage(findDetailPage(staffId, status, range, pageable));
    }

    @Caching(evict = {
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
    public int rebuildSummaries(CommissionPeriod period, LocalDate from, LocalDate to) {
        requireManagerOrAdmin(getCurrentUser());
        DateRange range = resolveDateRange(period, from, to);
        List<User> staffUsers = userRepository.findByRoleAndIsDeleteFalse(Role.STAFF);
        int refreshedDays = 0;
        for (User staff : staffUsers) {
            for (LocalDate date = range.start(); !date.isAfter(range.end()); date = date.plusDays(1)) {
                refreshDailySummary(staff.getId(), date);
                refreshedDays++;
            }
        }
        return refreshedDays;
    }

    public void refreshForAttributions(Collection<ConsultationSaleAttribution> attributions) {
        if (attributions == null || attributions.isEmpty()) {
            return;
        }

        Set<RefreshKey> refreshKeys = collectRefreshKeys(attributions);
        if (refreshKeys.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            Set<RefreshKey> keysAfterCommit = new HashSet<>(refreshKeys);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refreshKeys(keysAfterCommit);
                }
            });
            return;
        }

        refreshKeys(refreshKeys);
    }

    private Page<ConsultationSaleAttribution> findDetailPage(
            Long staffId,
            ConsultationAttributionStatus status,
            DateRange range,
            Pageable pageable
    ) {
        ConsultationAttributionStatus resolvedStatus = status == null ? ConsultationAttributionStatus.CONFIRMED : status;
        return switch (resolvedStatus) {
            case CONFIRMED -> attributionRepository
                    .findByStaffIdAndStatusAndConfirmedAtGreaterThanEqualAndConfirmedAtLessThanOrderByConfirmedAtDesc(
                            staffId,
                            resolvedStatus,
                            range.startDateTime(),
                            range.endExclusiveDateTime(),
                            pageable
                    );
            case PENDING -> attributionRepository
                    .findByStaffIdAndStatusAndOrderCreatedAtGreaterThanEqualAndOrderCreatedAtLessThanOrderByOrderCreatedAtDesc(
                            staffId,
                            resolvedStatus,
                            range.startDateTime(),
                            range.endExclusiveDateTime(),
                            pageable
                    );
            case CANCELLED -> attributionRepository
                    .findByStaffIdAndStatusAndCancelledAtGreaterThanEqualAndCancelledAtLessThanOrderByCancelledAtDesc(
                            staffId,
                            resolvedStatus,
                            range.startDateTime(),
                            range.endExclusiveDateTime(),
                            pageable
                    );
        };
    }

    private Page<StaffCommissionDetailDTO> toDetailPage(Page<ConsultationSaleAttribution> attributionPage) {
        List<Long> attributionIds = attributionPage.stream()
                .map(ConsultationSaleAttribution::getId)
                .filter(Objects::nonNull)
                .toList();
        Set<Long> reviewedIds = attributionIds.isEmpty()
                ? Set.of()
                : new HashSet<>(reviewRepository.findReviewedAttributionIds(attributionIds));
        return attributionPage.map(attribution -> toDetailDto(attribution, reviewedIds.contains(attribution.getId())));
    }

    private StaffCommissionDetailDTO toDetailDto(ConsultationSaleAttribution attribution, boolean reviewed) {
        User staff = attribution.getStaff();
        User customer = attribution.getUser();
        ConsultationRequest request = attribution.getConsultationRequest();
        OrderItem orderItem = attribution.getOrderItem();
        Product product = attribution.getProduct();
        ProductVariant variant = attribution.getProductVariant();

        return new StaffCommissionDetailDTO(
                attribution.getId(),
                staff.getId(),
                buildFullName(staff),
                customer.getId(),
                buildFullName(customer),
                attribution.getOrder().getId(),
                orderItem.getId(),
                product.getId(),
                product.getProductName(),
                variant.getId(),
                variant.getVariantName(),
                request.getId(),
                attribution.getConsultationCreatedAt(),
                resolveConsultationAcceptedAt(request),
                request.getFirstStaffReplyAt(),
                attribution.getOrderCreatedAt(),
                attribution.getConfirmedAt(),
                attribution.getCancelledAt(),
                orderItem.getQuantity(),
                orderItem.getReceivedQuantity(),
                attribution.getItemAmount(),
                attribution.getBonusPercent(),
                attribution.getBonusAmount(),
                attribution.getStatus(),
                reviewed
        );
    }

    private StaffCommissionSummaryDTO buildStaffSummary(User staff, DateRange range) {
        List<StaffCommissionDailySummary> summaries = summaryRepository.findByStaffIdAndSummaryDateBetween(
                staff.getId(),
                range.start(),
                range.end()
        );

        return new StaffCommissionSummaryDTO(
                staff.getId(),
                buildFullName(staff),
                staff.getAvatarUrl(),
                range.start(),
                range.end(),
                roundMoney(summaries.stream().mapToDouble(StaffCommissionDailySummary::getConfirmedCommissionAmount).sum()),
                roundMoney(summaries.stream().mapToDouble(StaffCommissionDailySummary::getConfirmedRevenueAmount).sum()),
                summaries.stream().mapToLong(StaffCommissionDailySummary::getConfirmedOrderCount).sum(),
                summaries.stream().mapToLong(StaffCommissionDailySummary::getConfirmedAttributionCount).sum(),
                roundMoney(summaries.stream().mapToDouble(StaffCommissionDailySummary::getPendingCommissionAmount).sum()),
                roundMoney(summaries.stream().mapToDouble(StaffCommissionDailySummary::getPendingRevenueAmount).sum()),
                summaries.stream().mapToLong(StaffCommissionDailySummary::getPendingOrderCount).sum(),
                summaries.stream().mapToLong(StaffCommissionDailySummary::getPendingAttributionCount).sum(),
                summaries.stream().mapToLong(StaffCommissionDailySummary::getCancelledAttributionCount).sum()
        );
    }

    private void refreshKeys(Collection<RefreshKey> refreshKeys) {
        for (RefreshKey refreshKey : refreshKeys) {
            refreshDailySummary(refreshKey.staffId(), refreshKey.summaryDate());
        }
    }

    private void refreshDailySummary(Long staffId, LocalDate summaryDate) {
        User staff = userRepository.findById(staffId).orElse(null);
        if (staff == null || staff.isDelete()) {
            summaryRepository.deleteByStaffIdAndSummaryDate(staffId, summaryDate);
            return;
        }

        LocalDateTime start = summaryDate.atStartOfDay();
        LocalDateTime end = summaryDate.plusDays(1).atStartOfDay();

        List<ConsultationSaleAttribution> confirmed = attributionRepository
                .findByStaffIdAndStatusAndConfirmedAtGreaterThanEqualAndConfirmedAtLessThan(
                        staffId,
                        ConsultationAttributionStatus.CONFIRMED,
                        start,
                        end
                );
        List<ConsultationSaleAttribution> pending = attributionRepository
                .findByStaffIdAndStatusAndOrderCreatedAtGreaterThanEqualAndOrderCreatedAtLessThan(
                        staffId,
                        ConsultationAttributionStatus.PENDING,
                        start,
                        end
                );
        List<ConsultationSaleAttribution> cancelled = attributionRepository
                .findByStaffIdAndStatusAndCancelledAtGreaterThanEqualAndCancelledAtLessThan(
                        staffId,
                        ConsultationAttributionStatus.CANCELLED,
                        start,
                        end
                );

        if (confirmed.isEmpty() && pending.isEmpty() && cancelled.isEmpty()) {
            summaryRepository.deleteByStaffIdAndSummaryDate(staffId, summaryDate);
            return;
        }

        StaffCommissionDailySummary summary = summaryRepository.findByStaffIdAndSummaryDate(staffId, summaryDate)
                .orElseGet(StaffCommissionDailySummary::new);
        summary.setId(buildSummaryId(staffId, summaryDate));
        summary.setStaffId(staffId);
        summary.setStaffName(buildFullName(staff));
        summary.setAvatarUrl(staff.getAvatarUrl());
        summary.setSummaryDate(summaryDate);
        summary.setConfirmedCommissionAmount(sumBonusAmount(confirmed));
        summary.setConfirmedRevenueAmount(sumItemAmount(confirmed));
        summary.setConfirmedOrderCount(countDistinctOrders(confirmed));
        summary.setConfirmedAttributionCount(confirmed.size());
        summary.setPendingCommissionAmount(sumBonusAmount(pending));
        summary.setPendingRevenueAmount(sumItemAmount(pending));
        summary.setPendingOrderCount(countDistinctOrders(pending));
        summary.setPendingAttributionCount(pending.size());
        summary.setCancelledAttributionCount(cancelled.size());
        summary.setUpdatedAt(LocalDateTime.now());
        summaryRepository.save(summary);
    }

    private Set<RefreshKey> collectRefreshKeys(Collection<ConsultationSaleAttribution> attributions) {
        Set<RefreshKey> refreshKeys = new HashSet<>();
        for (ConsultationSaleAttribution attribution : attributions) {
            if (attribution == null || attribution.getStaff() == null || attribution.getStaff().getId() == null) {
                continue;
            }
            Long staffId = attribution.getStaff().getId();
            collectRefreshKey(refreshKeys, staffId, attribution.getOrderCreatedAt());
            collectRefreshKey(refreshKeys, staffId, attribution.getConfirmedAt());
            collectRefreshKey(refreshKeys, staffId, attribution.getCancelledAt());
        }
        return refreshKeys;
    }

    private void collectRefreshKey(Set<RefreshKey> refreshKeys, Long staffId, LocalDateTime dateTime) {
        if (dateTime != null) {
            refreshKeys.add(new RefreshKey(staffId, dateTime.toLocalDate()));
        }
    }

    private DateRange resolveDateRange(CommissionPeriod period, LocalDate from, LocalDate to) {
        CommissionPeriod resolvedPeriod = period == null ? CommissionPeriod.MONTH : period;
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;

        if (from != null) {
            start = from;
            end = to == null ? defaultEndForPeriod(resolvedPeriod, from) : to;
        } else if (to != null) {
            end = to;
            start = defaultStartForPeriod(resolvedPeriod, to);
        } else {
            start = defaultStartForPeriod(resolvedPeriod, today);
            end = defaultEndForPeriod(resolvedPeriod, start);
        }

        validateDateRange(start, end);
        return new DateRange(start, end);
    }

    private LocalDate defaultStartForPeriod(CommissionPeriod period, LocalDate date) {
        return switch (period) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate defaultEndForPeriod(CommissionPeriod period, LocalDate start) {
        return switch (period) {
            case DAY -> start;
            case WEEK -> start.plusDays(6);
            case MONTH -> start.with(TemporalAdjusters.lastDayOfMonth());
        };
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Report start date must not be after end date.");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_REPORT_DAYS) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Report range must not exceed 370 days.");
        }
    }

    private LocalDateTime resolveConsultationAcceptedAt(ConsultationRequest request) {
        if (request.getClaimedAt() != null) {
            return request.getClaimedAt();
        }
        return request.getAssignedAt();
    }

    private long countDistinctOrders(List<ConsultationSaleAttribution> attributions) {
        return attributions.stream()
                .map(ConsultationSaleAttribution::getOrder)
                .filter(Objects::nonNull)
                .map(order -> order.getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private double sumBonusAmount(List<ConsultationSaleAttribution> attributions) {
        return roundMoney(attributions.stream()
                .mapToDouble(attribution -> safeAmount(attribution.getBonusAmount()))
                .sum());
    }

    private double sumItemAmount(List<ConsultationSaleAttribution> attributions) {
        return roundMoney(attributions.stream()
                .mapToDouble(attribution -> safeAmount(attribution.getItemAmount()))
                .sum());
    }

    private double safeAmount(Double amount) {
        if (amount == null || amount <= 0) {
            return 0;
        }
        return amount;
    }

    private double roundMoney(double value) {
        return Math.round(value * MONEY_ROUNDING_FACTOR) / MONEY_ROUNDING_FACTOR;
    }

    private String buildSummaryId(Long staffId, LocalDate summaryDate) {
        return staffId + ":" + summaryDate;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsernameAndIsDeleteFalse(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
    }

    private User getStaffOrThrow(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.STAFF_NOT_FOUND, staffId));
        if (staff.isDelete() || staff.getRole() != Role.STAFF) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.STAFF_NOT_FOUND, staffId);
        }
        return staff;
    }

    private User requireStaff(User user) {
        if (user.getRole() != Role.STAFF) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
        }
        return user;
    }

    private void requireManagerOrAdmin(User user) {
        if (user.getRole() != Role.MANAGER && user.getRole() != Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only manager or admin can view staff commission reports.");
        }
    }

    private String buildFullName(User user) {
        String fullName = Stream.of(user.getLastname(), user.getFirstname())
                .filter(part -> part != null && !part.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        if (!fullName.isBlank()) {
            return fullName;
        }
        return user.getUsername();
    }

    private record DateRange(LocalDate start, LocalDate end) {
        private LocalDateTime startDateTime() {
            return start.atStartOfDay();
        }

        private LocalDateTime endExclusiveDateTime() {
            return end.plusDays(1).atStartOfDay();
        }
    }

    private record RefreshKey(Long staffId, LocalDate summaryDate) {
    }
}
