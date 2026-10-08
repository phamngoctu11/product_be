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
import com.example.workflow.event.payload.CommissionRefreshKey;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.CommissionPeriod;
import com.example.workflow.nume.ConsultationAttributionStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ConsultationReviewRepository;
import com.example.workflow.repository.ConsultationSaleAttributionRepository;
import com.example.workflow.repository.StaffCommissionDailySummaryRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.util.MoneyUtils;
import com.example.workflow.util.CommissionRefreshKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final long MAX_REPORT_DAYS = 370;

    private final ConsultationSaleAttributionRepository attributionRepository;
    private final ConsultationReviewRepository reviewRepository;
    private final StaffCommissionDailySummaryRepository summaryRepository;
    private final UserRepository userRepository;
    private final ApplicationCacheService applicationCacheService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionSummaries",
            key = "'me-' + @currentUserService.requireCurrentUserId() + '-' + #period.name() + '-' + #from + '-' + #to"
    )
    public StaffCommissionSummaryDTO getMySummary(CommissionPeriod period, LocalDate from, LocalDate to) {
        User staff = currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
        DateRange range = resolveDateRange(period, from, to);
        return buildStaffSummary(staff, range);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionDetails",
            key = "'me-' + @currentUserService.requireCurrentUserId() + '-' + #period.name() + '-' + #from + '-' + #to + '-' + #status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public Page<StaffCommissionDetailDTO> getMyDetails(
            CommissionPeriod period,
            LocalDate from,
            LocalDate to,
            ConsultationAttributionStatus status,
            Pageable pageable
    ) {
        User staff = currentUserService.requireCurrentUser(Role.STAFF, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
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
        requireManagerOrAdmin();
        DateRange range = resolveDateRange(period, from, to);
        return userRepository.findByRoleAndIsDeleteFalse(Role.STAFF, pageable)
                .map(staff -> buildStaffSummary(staff, range));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionSummaries",
            key = "'staff-' + #staffId + '-' + #period.name() + '-' + #from + '-' + #to"
    )
    public StaffCommissionSummaryDTO getStaffSummary(String staffId, CommissionPeriod period, LocalDate from, LocalDate to) {
        requireManagerOrAdmin();
        User staff = userService.requireStaff(staffId);
        DateRange range = resolveDateRange(period, from, to);
        return buildStaffSummary(staff, range);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "staffCommissionDetails",
            key = "'staff-' + #staffId + '-' + #period.name() + '-' + #from + '-' + #to + '-' + #status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public Page<StaffCommissionDetailDTO> getStaffDetails(
            String staffId,
            CommissionPeriod period,
            LocalDate from,
            LocalDate to,
            ConsultationAttributionStatus status,
            Pageable pageable
    ) {
        requireManagerOrAdmin();
        userService.requireStaff(staffId);
        DateRange range = resolveDateRange(period, from, to);
        return toDetailPage(findDetailPage(staffId, status, range, pageable));
    }

    public int rebuildSummaries(CommissionPeriod period, LocalDate from, LocalDate to) {
        requireManagerOrAdmin();
        DateRange range = resolveDateRange(period, from, to);
        List<User> staffUsers = userRepository.findByRoleAndIsDeleteFalse(Role.STAFF);
        int refreshedDays = 0;
        for (User staff : staffUsers) {
            for (LocalDate date = range.start(); !date.isAfter(range.end()); date = date.plusDays(1)) {
                refreshDailySummary(staff.getId(), date);
                refreshedDays++;
            }
        }
        applicationCacheService.evictStaffCommissionRebuilt();
        return refreshedDays;
    }

    @Transactional
    public void refreshForAttributions(Collection<ConsultationSaleAttribution> attributions) {
        if (attributions == null || attributions.isEmpty()) {
            return;
        }

        Set<CommissionRefreshKey> refreshKeys = CommissionRefreshKeys.fromAttributions(attributions);
        if (refreshKeys.isEmpty()) {
            return;
        }
        refreshSummaries(refreshKeys);
    }

    @Transactional
    public void refreshSummaries(Collection<CommissionRefreshKey> commissionRefreshKeys) {
        if (commissionRefreshKeys == null || commissionRefreshKeys.isEmpty()) {
            return;
        }

        Set<RefreshKey> refreshKeys = new HashSet<>();
        for (CommissionRefreshKey key : commissionRefreshKeys) {
            if (key == null || key.staffId() == null || key.staffId().isBlank()
                    || key.summaryDate() == null || key.summaryDate().isBlank()) {
                continue;
            }
            try {
                refreshKeys.add(new RefreshKey(key.staffId(), LocalDate.parse(key.summaryDate())));
            } catch (RuntimeException ignored) {
                // Ignore malformed async refresh keys; the source attribution data remains the source of truth.
            }
        }

        if (refreshKeys.isEmpty()) {
            return;
        }

        refreshKeys(refreshKeys);
        applicationCacheService.evictStaffCommissionSummariesRefreshed();
    }

    private Page<ConsultationSaleAttribution> findDetailPage(
            String staffId,
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
                userService.displayName(staff),
                customer.getId(),
                userService.displayName(customer),
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
                userService.displayName(staff),
                staff.getAvatarUrl(),
                range.start(),
                range.end(),
                MoneyUtils.round(summaries.stream().mapToDouble(StaffCommissionDailySummary::getConfirmedCommissionAmount).sum()),
                MoneyUtils.round(summaries.stream().mapToDouble(StaffCommissionDailySummary::getConfirmedRevenueAmount).sum()),
                summaries.stream().mapToLong(StaffCommissionDailySummary::getConfirmedOrderCount).sum(),
                summaries.stream().mapToLong(StaffCommissionDailySummary::getConfirmedAttributionCount).sum(),
                MoneyUtils.round(summaries.stream().mapToDouble(StaffCommissionDailySummary::getPendingCommissionAmount).sum()),
                MoneyUtils.round(summaries.stream().mapToDouble(StaffCommissionDailySummary::getPendingRevenueAmount).sum()),
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

    private void refreshDailySummary(String staffId, LocalDate summaryDate) {
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
        summary.setStaffName(userService.displayName(staff));
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
        return MoneyUtils.round(attributions.stream()
                .mapToDouble(attribution -> safeAmount(attribution.getBonusAmount()))
                .sum());
    }

    private double sumItemAmount(List<ConsultationSaleAttribution> attributions) {
        return MoneyUtils.round(attributions.stream()
                .mapToDouble(attribution -> safeAmount(attribution.getItemAmount()))
                .sum());
    }

    private double safeAmount(Double amount) {
        if (amount == null || amount <= 0) {
            return 0;
        }
        return amount;
    }

    private String buildSummaryId(String staffId, LocalDate summaryDate) {
        return staffId + ":" + summaryDate;
    }

    private void requireManagerOrAdmin() {
        currentUserService.requireCurrentUser(
                Set.of(Role.MANAGER, Role.ADMIN),
                ConstantErrorCode.BAD_REQUEST_DETAIL,
                "Only manager or admin can view staff commission reports."
        );
    }

    private record DateRange(LocalDate start, LocalDate end) {
        private LocalDateTime startDateTime() {
            return start.atStartOfDay();
        }

        private LocalDateTime endExclusiveDateTime() {
            return end.plusDays(1).atStartOfDay();
        }
    }

    private record RefreshKey(String staffId, LocalDate summaryDate) {
    }
}
