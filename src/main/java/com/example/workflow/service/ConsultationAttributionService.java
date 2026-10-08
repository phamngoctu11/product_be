package com.example.workflow.service;

import com.example.workflow.dto.ConsultationReviewDTO;
import com.example.workflow.dto.ConsultationReviewRequest;
import com.example.workflow.dto.ConsultationSaleAttributionDTO;
import com.example.workflow.entity.ConsultationRequest;
import com.example.workflow.entity.ConsultationReview;
import com.example.workflow.entity.ConsultationSaleAttribution;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.ProductVariant;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.CommissionRefreshKey;
import com.example.workflow.event.payload.StaffCommissionRefreshRequestedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.ConsultationMapper;
import com.example.workflow.nume.ConsultationAttributionStatus;
import com.example.workflow.nume.ConsultationStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ConsultationRequestRepository;
import com.example.workflow.repository.ConsultationReviewRepository;
import com.example.workflow.repository.ConsultationSaleAttributionRepository;
import com.example.workflow.service.cache.ApplicationCacheService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.MoneyUtils;
import com.example.workflow.util.CommissionRefreshKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConsultationAttributionService {
    private static final Duration ATTRIBUTION_WINDOW = Duration.ofDays(2);
    private static final List<ConsultationStatus> ATTRIBUTABLE_STATUSES = List.of(
            ConsultationStatus.ASSIGNED,
            ConsultationStatus.IN_PROGRESS,
            ConsultationStatus.CLOSED
    );

    private final ConsultationSaleAttributionRepository attributionRepository;
    private final ConsultationReviewRepository reviewRepository;
    private final ConsultationRequestRepository consultationRepository;
    private final DomainEventPublisher eventPublisher;
    private final ApplicationCacheService applicationCacheService;
    private final CurrentUserService currentUserService;
    private final ConsultationMapper consultationMapper;

    @Value("${consultation.bonus.percent:5}")
    private double consultationBonusPercent;

    @Transactional
    public void recordOrderAttributions(Order order) {
        if (order == null || order.getUser() == null || order.getItems() == null || order.getStartOrderTime() == null) {
            return;
        }

        LocalDateTime orderCreatedAt = order.getStartOrderTime();
        LocalDateTime from = orderCreatedAt.minus(ATTRIBUTION_WINDOW);
        double orderGrossAmount = calculateOrderGrossAmount(order, false);
        List<OrderItem> attributableItems = order.getItems().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .filter(item -> resolveProduct(item) != null)
                .toList();
        if (attributableItems.isEmpty()) {
            return;
        }

        Set<Long> existingOrderItemIds = new HashSet<>(attributionRepository.findExistingOrderItemIds(
                attributableItems.stream().map(OrderItem::getId).toList()
        ));
        List<Long> productIds = attributableItems.stream()
                .map(this::resolveProduct)
                .map(Product::getId)
                .distinct()
                .toList();
        Map<Long, ConsultationRequest> latestConsultationByProductId = loadLatestConsultationsByProduct(
                order.getUser().getId(),
                productIds,
                from,
                orderCreatedAt
        );

        List<ConsultationSaleAttribution> savedAttributions = new ArrayList<>();
        for (OrderItem item : attributableItems) {
            if (existingOrderItemIds.contains(item.getId())) {
                continue;
            }
            Product product = resolveProduct(item);
            ConsultationRequest request = latestConsultationByProductId.get(product.getId());
            if (request != null) {
                savedAttributions.add(attributionRepository.save(toAttribution(order, item, request, orderCreatedAt, orderGrossAmount)));
            }
        }
        requestCommissionRefresh(savedAttributions);
        if (!savedAttributions.isEmpty()) {
            applicationCacheService.evictConsultationAttributionsRecorded();
        }
    }

    @Transactional
    public void confirmOrderAttributions(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        Double confirmedOrderGrossAmount = null;
        List<ConsultationSaleAttribution> updatedAttributions = new ArrayList<>();
        for (ConsultationSaleAttribution attribution : attributionRepository.findByOrderId(orderId)) {
            if (attribution.getStatus() == ConsultationAttributionStatus.PENDING) {
                if (confirmedOrderGrossAmount == null) {
                    confirmedOrderGrossAmount = calculateOrderGrossAmount(attribution.getOrder(), true);
                }
                applyCommissionSnapshot(attribution, confirmedOrderGrossAmount, true);
                attribution.setStatus(ConsultationAttributionStatus.CONFIRMED);
                attribution.setConfirmedAt(now);
                attribution.setCancelledAt(null);
                updatedAttributions.add(attribution);
            }
        }
        requestCommissionRefresh(updatedAttributions);
        if (!updatedAttributions.isEmpty()) {
            applicationCacheService.evictConsultationAttributionsConfirmed();
        }
    }

    @Transactional
    public void cancelOrderAttributions(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        List<ConsultationSaleAttribution> updatedAttributions = new ArrayList<>();
        for (ConsultationSaleAttribution attribution : attributionRepository.findByOrderId(orderId)) {
            if (attribution.getStatus() != ConsultationAttributionStatus.CANCELLED) {
                attribution.setStatus(ConsultationAttributionStatus.CANCELLED);
                attribution.setBonusEligible(false);
                attribution.setBonusAmount(0.0);
                attribution.setCancelledAt(now);
                updatedAttributions.add(attribution);
            }
        }
        requestCommissionRefresh(updatedAttributions);
        if (!updatedAttributions.isEmpty()) {
            applicationCacheService.evictConsultationAttributionsCancelled();
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "consultationAttributions",
            key = "'me-' + @currentUserService.requireCurrentUserId() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public Page<ConsultationSaleAttributionDTO> getMyAttributions(Pageable pageable) {
        User user = currentUserService.requireCurrentUser();
        if (user.getRole() == Role.STAFF) {
            return mapAttributions(attributionRepository
                    .findByStaffIdAndStatusInOrderByCreatedAtDesc(user.getId(), List.of(ConsultationAttributionStatus.PENDING, ConsultationAttributionStatus.CONFIRMED), pageable));
        }
        return mapAttributions(attributionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "consultationAttributions", key = "'staff-' + #staffId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ConsultationSaleAttributionDTO> getStaffAttributions(String staffId, Pageable pageable) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != Role.MANAGER && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only manager or admin can view staff attribution reports.");
        }
        return mapAttributions(attributionRepository
                .findByStaffIdAndStatusInOrderByCreatedAtDesc(staffId, List.of(ConsultationAttributionStatus.PENDING, ConsultationAttributionStatus.CONFIRMED), pageable));
    }

    @Transactional
    public ConsultationReviewDTO createReview(Long attributionId, ConsultationReviewRequest request) {
        User user = currentUserService.requireCurrentUser();
        if (user.getRole() != Role.USER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only customers can review consultation sales.");
        }

        ConsultationSaleAttribution attribution = attributionRepository.findByIdAndUserId(attributionId, user.getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation attribution not found."));
        if (attribution.getStatus() != ConsultationAttributionStatus.CONFIRMED) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only delivered consultation purchases can be reviewed.");
        }
        if (reviewRepository.existsByAttributionId(attributionId)) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "This consultation purchase has already been reviewed.");
        }

        ConsultationReview review = toReview(attribution, request);
        ConsultationReviewDTO response = consultationMapper.toDto(reviewRepository.save(review));
        applicationCacheService.evictConsultationReviewCreated();
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "consultationReviews", key = "'product-' + #productId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ConsultationReviewDTO> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable).map(consultationMapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "consultationReviews", key = "'staff-' + #staffId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ConsultationReviewDTO> getStaffReviews(String staffId, Pageable pageable) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == Role.STAFF && !currentUser.getId().equals(staffId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Staff can only view their own reviews.");
        }
        return reviewRepository.findByStaffIdOrderByCreatedAtDesc(staffId, pageable).map(consultationMapper::toDto);
    }

    private ConsultationSaleAttribution toAttribution(
            Order order,
            OrderItem item,
            ConsultationRequest request,
            LocalDateTime orderCreatedAt,
            double orderGrossAmount
    ) {
        ProductVariant variant = item.getProductVariant();
        Product product = variant.getProduct();

        ConsultationSaleAttribution attribution = new ConsultationSaleAttribution();
        attribution.setConsultationRequest(request);
        attribution.setOrder(order);
        attribution.setOrderItem(item);
        attribution.setUser(order.getUser());
        attribution.setStaff(request.getAssignedStaff());
        attribution.setProduct(product);
        attribution.setProductVariant(variant);
        attribution.setConsultationCreatedAt(request.getCreatedAt());
        attribution.setOrderCreatedAt(orderCreatedAt);
        attribution.setMinutesFromConsultationToOrder(Duration.between(request.getCreatedAt(), orderCreatedAt).toMinutes());
        attribution.setStatus(ConsultationAttributionStatus.PENDING);
        attribution.setBonusPercent(normalizePercent(consultationBonusPercent));
        applyCommissionSnapshot(attribution, orderGrossAmount, false);
        return attribution;
    }

    private ConsultationReview toReview(ConsultationSaleAttribution attribution, ConsultationReviewRequest request) {
        ConsultationReview review = new ConsultationReview();
        review.setAttribution(attribution);
        review.setConsultationRequest(attribution.getConsultationRequest());
        review.setOrder(attribution.getOrder());
        review.setOrderItem(attribution.getOrderItem());
        review.setUser(attribution.getUser());
        review.setStaff(attribution.getStaff());
        review.setProduct(attribution.getProduct());
        review.setProductRating(request.getProductRating());
        review.setStaffRating(request.getStaffRating());
        review.setComment(normalizeComment(request.getComment()));
        return review;
    }

    private void applyCommissionSnapshot(
            ConsultationSaleAttribution attribution,
            double orderGrossAmount,
            boolean preferReceivedQuantity
    ) {
        double commissionBase = calculateCommissionBase(
                attribution.getOrder(),
                attribution.getOrderItem(),
                orderGrossAmount,
                preferReceivedQuantity
        );
        double bonusPercent = normalizePercent(attribution.getBonusPercent() != null ? attribution.getBonusPercent() : consultationBonusPercent);
        boolean eligible = commissionBase > 0 && bonusPercent > 0;

        attribution.setItemAmount(commissionBase);
        attribution.setBonusPercent(bonusPercent);
        attribution.setBonusEligible(eligible);
        attribution.setBonusAmount(eligible ? calculateBonusAmount(commissionBase, bonusPercent) : 0.0);
    }

    private double calculateBonusAmount(double itemAmount, double bonusPercent) {
        if (bonusPercent <= 0 || itemAmount <= 0) {
            return 0;
        }
        return MoneyUtils.round(itemAmount * bonusPercent / 100);
    }

    private double calculateCommissionBase(
            Order order,
            OrderItem item,
            double orderGrossAmount,
            boolean preferReceivedQuantity
    ) {
        double itemGrossAmount = calculateItemGrossAmount(item, preferReceivedQuantity);
        if (itemGrossAmount <= 0) {
            return 0;
        }

        double discountAmount = normalizeAmount(order == null ? null : order.getDiscountAmount());
        if (discountAmount <= 0 || orderGrossAmount <= 0) {
            return MoneyUtils.round(itemGrossAmount);
        }

        double discountShare = discountAmount * itemGrossAmount / orderGrossAmount;
        discountShare = Math.min(itemGrossAmount, Math.max(0, discountShare));
        return MoneyUtils.round(Math.max(0, itemGrossAmount - discountShare));
    }

    private double calculateOrderGrossAmount(Order order, boolean preferReceivedQuantity) {
        if (order == null || order.getItems() == null) {
            return 0;
        }
        return order.getItems().stream()
                .filter(Objects::nonNull)
                .mapToDouble(item -> calculateItemGrossAmount(item, preferReceivedQuantity))
                .sum();
    }

    private double calculateItemGrossAmount(OrderItem item, boolean preferReceivedQuantity) {
        if (item == null) {
            return 0;
        }
        int quantity = resolveCommissionQuantity(item, preferReceivedQuantity);
        if (quantity <= 0 || item.getPrice() == null || item.getPrice() <= 0) {
            return 0;
        }
        return item.getPrice() * quantity;
    }

    private int resolveCommissionQuantity(OrderItem item, boolean preferReceivedQuantity) {
        if (preferReceivedQuantity && item.getReceivedQuantity() != null) {
            return Math.max(0, item.getReceivedQuantity());
        }
        return Math.max(0, item.getQuantity());
    }

    private double normalizeAmount(Double amount) {
        if (amount == null || amount <= 0) {
            return 0;
        }
        return amount;
    }

    private double normalizePercent(Double percent) {
        if (percent == null || percent <= 0) {
            return 0;
        }
        return percent;
    }

    private Map<Long, ConsultationRequest> loadLatestConsultationsByProduct(
            String userId,
            List<Long> productIds,
            LocalDateTime from,
            LocalDateTime orderCreatedAt
    ) {
        Map<Long, ConsultationRequest> latestByProductId = new HashMap<>();
        if (productIds.isEmpty()) {
            return latestByProductId;
        }

        for (ConsultationRequest request : consultationRepository.findAttributionCandidates(
                userId,
                productIds,
                ATTRIBUTABLE_STATUSES,
                from,
                orderCreatedAt
        )) {
            if (!isAttributableBeforeOrder(request, orderCreatedAt)) {
                continue;
            }
            Long productId = request.getProduct().getId();
            latestByProductId.merge(productId, request, this::pickLatestConsultation);
        }
        return latestByProductId;
    }

    private ConsultationRequest pickLatestConsultation(ConsultationRequest left, ConsultationRequest right) {
        if (left.getCreatedAt().isAfter(right.getCreatedAt())) {
            return left;
        }
        return right;
    }

    private Product resolveProduct(OrderItem item) {
        ProductVariant variant = item.getProductVariant();
        if (variant == null || variant.getProduct() == null || variant.getProduct().getId() == null) {
            return null;
        }
        return variant.getProduct();
    }

    private boolean isWithinAttributionWindow(LocalDateTime consultationCreatedAt, LocalDateTime orderCreatedAt) {
        Duration duration = Duration.between(consultationCreatedAt, orderCreatedAt);
        return !duration.isNegative() && duration.compareTo(ATTRIBUTION_WINDOW) < 0;
    }

    private boolean isAttributableBeforeOrder(ConsultationRequest request, LocalDateTime orderCreatedAt) {
        return request.getFirstStaffReplyAt() != null
                && !request.getFirstStaffReplyAt().isAfter(orderCreatedAt)
                && isWithinAttributionWindow(request.getCreatedAt(), orderCreatedAt);
    }

    private Page<ConsultationSaleAttributionDTO> mapAttributions(Page<ConsultationSaleAttribution> page) {
        List<Long> ids = page.getContent().stream()
                .map(ConsultationSaleAttribution::getId)
                .filter(Objects::nonNull)
                .toList();
        Set<Long> reviewedIds = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(reviewRepository.findReviewedAttributionIds(ids));
        return page.map(attribution -> consultationMapper.toDto(
                attribution,
                reviewedIds.contains(attribution.getId())
        ));
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }

    private void requestCommissionRefresh(Collection<ConsultationSaleAttribution> attributions) {
        if (attributions == null || attributions.isEmpty()) {
            return;
        }

        Set<CommissionRefreshKey> refreshKeys = CommissionRefreshKeys.fromAttributions(attributions);

        if (!refreshKeys.isEmpty()) {
            eventPublisher.publishAfterCommit(
                    EventTypes.STAFF_COMMISSION_REFRESH_REQUESTED,
                    new StaffCommissionRefreshRequestedEvent(refreshKeys)
            );
        }
    }

}
