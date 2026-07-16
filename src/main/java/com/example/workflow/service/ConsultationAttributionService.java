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
import com.example.workflow.event.DomainEventPublisher;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.CommissionRefreshKey;
import com.example.workflow.event.payload.StaffCommissionRefreshRequestedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.ConsultationAttributionStatus;
import com.example.workflow.nume.ConsultationStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ConsultationRequestRepository;
import com.example.workflow.repository.ConsultationReviewRepository;
import com.example.workflow.repository.ConsultationSaleAttributionRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private static final double MONEY_ROUNDING_FACTOR = 100.0;

    private final ConsultationSaleAttributionRepository attributionRepository;
    private final ConsultationReviewRepository reviewRepository;
    private final ConsultationRequestRepository consultationRepository;
    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;

    @Value("${consultation.bonus.percent:5}")
    private double consultationBonusPercent;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "consultationAttributions", allEntries = true),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
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
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "consultationAttributions", allEntries = true),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
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
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "consultationAttributions", allEntries = true),
            @CacheEvict(value = "staffCommissionSummaries", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true)
    })
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
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "consultationAttributions",
            key = "'me-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()"
    )
    public Page<ConsultationSaleAttributionDTO> getMyAttributions(Pageable pageable) {
        User user = getCurrentUser();
        if (user.getRole() == Role.STAFF) {
            return attributionRepository
                    .findByStaffIdAndStatusInOrderByCreatedAtDesc(user.getId(), List.of(ConsultationAttributionStatus.PENDING, ConsultationAttributionStatus.CONFIRMED), pageable)
                    .map(this::toDto);
        }
        return attributionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "consultationAttributions", key = "'staff-' + #staffId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ConsultationSaleAttributionDTO> getStaffAttributions(String staffId, Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.MANAGER && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only manager or admin can view staff attribution reports.");
        }
        return attributionRepository
                .findByStaffIdAndStatusInOrderByCreatedAtDesc(staffId, List.of(ConsultationAttributionStatus.PENDING, ConsultationAttributionStatus.CONFIRMED), pageable)
                .map(this::toDto);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "consultationAttributions", allEntries = true),
            @CacheEvict(value = "staffCommissionDetails", allEntries = true),
            @CacheEvict(value = "consultationReviews", allEntries = true),
            @CacheEvict(value = "product", allEntries = true),
            @CacheEvict(value = "products", allEntries = true)
    })
    public ConsultationReviewDTO createReview(Long attributionId, ConsultationReviewRequest request) {
        User user = getCurrentUser();
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
        return toReviewDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "consultationReviews", key = "'product-' + #productId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ConsultationReviewDTO> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable).map(this::toReviewDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "consultationReviews", key = "'staff-' + #staffId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ConsultationReviewDTO> getStaffReviews(String staffId, Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.STAFF && !currentUser.getId().equals(staffId)) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Staff can only view their own reviews.");
        }
        return reviewRepository.findByStaffIdOrderByCreatedAtDesc(staffId, pageable).map(this::toReviewDto);
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
        return roundMoney(itemAmount * bonusPercent / 100);
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
            return roundMoney(itemGrossAmount);
        }

        double discountShare = discountAmount * itemGrossAmount / orderGrossAmount;
        discountShare = Math.min(itemGrossAmount, Math.max(0, discountShare));
        return roundMoney(Math.max(0, itemGrossAmount - discountShare));
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
        if (quantity <= 0 || item.getPrice() <= 0) {
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

    private double roundMoney(double value) {
        return Math.round(value * MONEY_ROUNDING_FACTOR) / MONEY_ROUNDING_FACTOR;
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

    private ConsultationSaleAttributionDTO toDto(ConsultationSaleAttribution attribution) {
        Product product = attribution.getProduct();
        ProductVariant variant = attribution.getProductVariant();
        User staff = attribution.getStaff();
        return new ConsultationSaleAttributionDTO(
                attribution.getId(),
                attribution.getConsultationRequest().getId(),
                attribution.getOrder().getId(),
                attribution.getOrderItem().getId(),
                attribution.getUser().getId(),
                staff.getId(),
                buildFullName(staff),
                product.getId(),
                product.getProductName(),
                variant.getId(),
                variant.getVariantName(),
                attribution.getConsultationCreatedAt(),
                attribution.getOrderCreatedAt(),
                attribution.getMinutesFromConsultationToOrder(),
                attribution.getItemAmount(),
                attribution.isBonusEligible(),
                attribution.getBonusPercent(),
                attribution.getBonusAmount(),
                attribution.getStatus(),
                attribution.getCreatedAt(),
                attribution.getConfirmedAt(),
                attribution.getCancelledAt(),
                reviewRepository.existsByAttributionId(attribution.getId())
        );
    }

    private ConsultationReviewDTO toReviewDto(ConsultationReview review) {
        User staff = review.getStaff();
        Product product = review.getProduct();
        return new ConsultationReviewDTO(
                review.getId(),
                review.getAttribution().getId(),
                review.getConsultationRequest().getId(),
                review.getOrder().getId(),
                review.getOrderItem().getId(),
                review.getUser().getId(),
                staff.getId(),
                buildFullName(staff),
                product.getId(),
                product.getProductName(),
                review.getProductRating(),
                review.getStaffRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
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

        Set<CommissionRefreshKey> refreshKeys = new HashSet<>();
        for (ConsultationSaleAttribution attribution : attributions) {
            if (attribution == null || attribution.getStaff() == null || attribution.getStaff().getId() == null) {
                continue;
            }
            String staffId = attribution.getStaff().getId();
            collectRefreshKey(refreshKeys, staffId, attribution.getOrderCreatedAt());
            collectRefreshKey(refreshKeys, staffId, attribution.getConfirmedAt());
            collectRefreshKey(refreshKeys, staffId, attribution.getCancelledAt());
        }

        if (!refreshKeys.isEmpty()) {
            eventPublisher.publishAfterCommit(
                    EventTypes.STAFF_COMMISSION_REFRESH_REQUESTED,
                    new StaffCommissionRefreshRequestedEvent(refreshKeys)
            );
        }
    }

    private void collectRefreshKey(Set<CommissionRefreshKey> refreshKeys, String staffId, LocalDateTime dateTime) {
        if (dateTime != null) {
            refreshKeys.add(new CommissionRefreshKey(staffId, dateTime.toLocalDate().toString()));
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
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
}
