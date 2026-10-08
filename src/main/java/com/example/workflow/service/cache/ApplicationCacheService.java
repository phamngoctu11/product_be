package com.example.workflow.service.cache;

import com.example.workflow.cache.CacheKeys;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.entity.Order;
import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.redis.DeferredCacheEvictionPublisher;
import com.example.workflow.service.redis.OptionalCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationCacheService {
    private final OptionalCacheService optionalCacheService;
    private final DeferredCacheEvictionPublisher cacheEvictionPublisher;

    public void evictCartChanged(String cartCacheKey) {
        if (StringUtils.hasText(cartCacheKey)) {
            optionalCacheService.evictAfterCommit(CacheNames.CARTS, cartCacheKey);
        }
    }

    public void evictUserCartChanged(String userId) {
        if (StringUtils.hasText(userId)) {
            evictCartChanged("user-" + userId);
        }
    }

    public void evictGuestCheckoutCart(String guestSessionId) {
        if (StringUtils.hasText(guestSessionId)) {
            evictCartChanged("guest-" + guestSessionId.trim());
        }
    }

    public void evictUserCheckoutCart(String userId) {
        evictUserCartChanged(userId);
    }

    public void evictOrderCreated(Order order) {
        String userId = orderUserId(order);
        evictUserOrdersAfterCommit(userId);
        if (order != null && order.getUserVoucher() != null) {
            evictUserVoucherWalletAfterCommit(userId);
        }
        if (order != null && order.getGuestVoucherTemplate() != null) {
            optionalCacheService.clearAfterCommit(CacheNames.GUEST_VOUCHER_TEMPLATES);
            optionalCacheService.clearAfterCommit(CacheNames.VOUCHER_TEMPLATES);
        }
    }

    public void evictWarehouseClaimed(Order order, String staffId) {
        optionalCacheService.clearAfterCommit(CacheNames.WAREHOUSE_PENDING_ORDERS);
        evictStaffAssignedOrdersAfterCommit(staffId);
        evictUserOrdersAfterCommit(orderUserId(order));
    }

    public void evictStaffAssigned(Order order, OrderStatus oldStatus, String previousStaffId, String assignedStaffId) {
        evictManagerPendingOrdersAfterCommit(oldStatus);
        if (oldStatus == OrderStatus.PENDING_WAREHOUSE) {
            optionalCacheService.clearAfterCommit(CacheNames.WAREHOUSE_PENDING_ORDERS);
        }
        evictStaffAssignedOrdersAfterCommit(previousStaffId);
        evictStaffAssignedOrdersAfterCommit(assignedStaffId);
        evictUserOrdersAfterCommit(orderUserId(order));
    }

    public void evictManagerReviewed(Order order, boolean approved, String assignedStaffId) {
        evictManagerPendingOrdersAfterCommit(OrderStatus.PENDING_APPROVAL);
        if (approved && !StringUtils.hasText(assignedStaffId)) {
            optionalCacheService.clearAfterCommit(CacheNames.WAREHOUSE_PENDING_ORDERS);
        }
        if (approved) {
            evictStaffAssignedOrdersAfterCommit(assignedStaffId);
        }
        evictUserOrdersAfterCommit(orderUserId(order));
    }

    public void evictStaffExported(Order order, String staffId) {
        evictStaffAssignedOrdersAfterCommit(staffId);
        evictManagerPendingOrdersAfterCommit(OrderStatus.PENDING_KCS);
        evictUserOrdersAfterCommit(orderUserId(order));
    }

    public void evictManagerKcsChecked(Order order) {
        evictManagerPendingOrdersAfterCommit(OrderStatus.PENDING_KCS);
        evictStaffAssignedOrdersAfterCommit(orderStaffId(order));
        evictUserOrdersAfterCommit(orderUserId(order));
    }

    public void evictOrderDelivered(Order order, String userId) {
        evictUserOrdersAfterCommit(userId);
        evictUserStateAfterCommit(userId);
    }

    public void evictOrderCancelled(Order order, OrderStatus oldStatus) {
        String userId = orderUserId(order);
        evictUserOrdersAfterCommit(userId);
        evictUserCancelledOrdersAfterCommit(userId);
        evictUserVoucherWalletAfterCommit(userId);
        evictUserStateAfterCommit(userId);
        evictManagerPendingOrdersAfterCommit(oldStatus);
        if (oldStatus == OrderStatus.PENDING_WAREHOUSE) {
            optionalCacheService.clearAfterCommit(CacheNames.WAREHOUSE_PENDING_ORDERS);
        }
        evictStaffAssignedOrdersAfterCommit(orderStaffId(order));
        if (order != null && order.getGuestVoucherTemplate() != null) {
            optionalCacheService.clearAfterCommit(CacheNames.GUEST_VOUCHER_TEMPLATES);
            optionalCacheService.clearAfterCommit(CacheNames.VOUCHER_TEMPLATES);
        }
    }

    public void evictProductCreated() {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        publishDeferredCacheEvictions("product created", wishlistEntries());
    }

    public void evictProductUpdated(Long productId) {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.evictAfterCommit(CacheNames.PRODUCT, productId);
        publishDeferredCacheEvictions(
                "product updated",
                CacheEvictionEntry.allEntries(CacheNames.STAFF_COMMISSION_DETAILS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_STATUS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_STATUS_BATCH)
        );
    }

    public void evictProductBasicInfoUpdated(Long productId) {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.evictAfterCommit(CacheNames.PRODUCT, productId);
        publishDeferredCacheEvictions(
                "product basic info updated",
                CacheEvictionEntry.allEntries(CacheNames.STAFF_COMMISSION_DETAILS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_STATUS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_STATUS_BATCH)
        );
    }

    public void evictProductVariantAdded(Long productId) {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.evictAfterCommit(CacheNames.PRODUCT, productId);
        publishDeferredCacheEvictions("product variant added", wishlistEntries());
    }

    public void evictStockImported() {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCT);
        publishDeferredCacheEvictions("stock imported", wishlistEntries());
    }

    public void evictProductDeleted(Long productId) {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.evictAfterCommit(CacheNames.PRODUCT, productId);
        publishDeferredCacheEvictions("product deleted", wishlistEntries());
    }

    public void evictWishlistChanged(String userId, Long productId) {
        optionalCacheService.clearAfterCommit(CacheNames.WISHLIST_PRODUCTS);
        if (StringUtils.hasText(userId) && productId != null) {
            optionalCacheService.evictAfterCommit(CacheNames.WISHLIST_STATUS, userId + "-" + productId);
        }
        optionalCacheService.clearAfterCommit(CacheNames.WISHLIST_STATUS_BATCH);
    }

    public void evictVoucherRedeemed(String userId) {
        if (StringUtils.hasText(userId)) {
            optionalCacheService.evictAfterCommit(CacheNames.USER, userId);
            optionalCacheService.evictAfterCommit(CacheNames.USER_VOUCHER_WALLET, userId);
        }
        optionalCacheService.clearAfterCommit(CacheNames.VOUCHER_TEMPLATES);
    }

    public void evictVoucherCampaignChanged() {
        optionalCacheService.clearAfterCommit(CacheNames.VOUCHER_TEMPLATES);
        optionalCacheService.clearAfterCommit(CacheNames.GUEST_VOUCHER_TEMPLATES);
    }

    public void evictReputationChanged(String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        optionalCacheService.evictByPrefixAfterCommit(CacheNames.REPUTATION_HISTORIES, CacheKeys.reputationHistoriesPrefix(userId));
        optionalCacheService.evictAfterCommit(CacheNames.USER, userId);
        optionalCacheService.clearAfterCommit(CacheNames.USERS);
    }

    public void evictProductReviewChanged() {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCT_REVIEWS);
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCT_REVIEW_SUMMARIES);
    }

    public void evictProductReviewChangedByUser(String userId) {
        evictProductReviewChanged();
        evictUserOrdersAfterCommit(userId);
    }

    public void evictUserRegistered() {
        optionalCacheService.clearAfterCommit(CacheNames.USERS);
    }

    public void evictMyProfileUpdated() {
        optionalCacheService.clearAfterCommit(CacheNames.USERS);
        optionalCacheService.clearAfterCommit(CacheNames.USER);
        publishDeferredCacheEvictions("my profile updated", staffCommissionEntries());
    }

    public void evictUserUpdated(String userId) {
        optionalCacheService.clearAfterCommit(CacheNames.USERS);
        optionalCacheService.evictAfterCommit(CacheNames.USER, userId);
        publishDeferredCacheEvictions("user updated", staffCommissionEntries());
    }

    public void evictUserDeleted(String userId) {
        optionalCacheService.clearAfterCommit(CacheNames.USERS);
        optionalCacheService.evictAfterCommit(CacheNames.USER, userId);
        publishDeferredCacheEvictions("user deleted", staffCommissionEntries());
    }

    public void evictConsultationAttributionsRecorded() {
        publishDeferredCacheEvictions("consultation order attributions recorded", consultationAttributionEntries());
    }

    public void evictConsultationAttributionsConfirmed() {
        publishDeferredCacheEvictions("consultation order attributions confirmed", consultationAttributionEntries());
    }

    public void evictConsultationAttributionsCancelled() {
        publishDeferredCacheEvictions("consultation order attributions cancelled", consultationAttributionEntries());
    }

    public void evictConsultationReviewCreated() {
        optionalCacheService.clearAfterCommit(CacheNames.CONSULTATION_ATTRIBUTIONS);
        optionalCacheService.clearAfterCommit(CacheNames.CONSULTATION_REVIEWS);
        publishDeferredCacheEvictions(
                "consultation review created",
                CacheEvictionEntry.allEntries(CacheNames.STAFF_COMMISSION_DETAILS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS)
        );
    }

    public void evictStaffCommissionRebuilt() {
        optionalCacheService.clearAfterCommit(CacheNames.STAFF_COMMISSION_SUMMARIES);
        optionalCacheService.clearAfterCommit(CacheNames.STAFF_COMMISSION_DETAILS);
    }

    public void evictStaffCommissionSummariesRefreshed() {
        publishDeferredCacheEvictions("staff commission summaries refreshed", staffCommissionEntries());
    }

    private void evictManagerPendingOrdersAfterCommit(OrderStatus status) {
        if (status == null) {
            return;
        }
        optionalCacheService.evictByPrefixAfterCommit(
                CacheNames.MANAGER_PENDING_ORDERS,
                CacheKeys.managerPendingOrdersPrefix(status)
        );
    }

    private void evictUserOrdersAfterCommit(String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        optionalCacheService.evictByPrefixAfterCommit(CacheNames.USER_ORDERS, CacheKeys.userOrdersPrefix(userId));
    }

    private void evictUserCancelledOrdersAfterCommit(String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        optionalCacheService.evictByPrefixAfterCommit(CacheNames.USER_CANCELLED_ORDERS, CacheKeys.userCancelledOrdersPrefix(userId));
    }

    private void evictStaffAssignedOrdersAfterCommit(String staffId) {
        if (!StringUtils.hasText(staffId)) {
            return;
        }
        optionalCacheService.evictByPrefixAfterCommit(CacheNames.STAFF_ASSIGNED_ORDERS, CacheKeys.staffAssignedOrdersPrefix(staffId));
    }

    private void evictUserStateAfterCommit(String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        optionalCacheService.evictAfterCommit(CacheNames.USER, userId);
        optionalCacheService.evictByPrefixAfterCommit(CacheNames.REPUTATION_HISTORIES, CacheKeys.reputationHistoriesPrefix(userId));
    }

    private void evictUserVoucherWalletAfterCommit(String userId) {
        if (StringUtils.hasText(userId)) {
            optionalCacheService.evictAfterCommit(CacheNames.USER_VOUCHER_WALLET, userId);
        }
    }

    private void publishDeferredCacheEvictions(String reason, CacheEvictionEntry... entries) {
        if (entries == null || entries.length == 0) {
            return;
        }
        List<CacheEvictionEntry> validEntries = new ArrayList<>();
        for (CacheEvictionEntry entry : entries) {
            if (entry != null) {
                validEntries.add(entry);
            }
        }
        publishDeferredCacheEvictions(reason, validEntries);
    }

    private void publishDeferredCacheEvictions(String reason, Collection<CacheEvictionEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        List<CacheEvictionEntry> validEntries = entries.stream()
                .filter(entry -> entry != null && StringUtils.hasText(entry.cacheName()))
                .toList();
        cacheEvictionPublisher.publishEventually(reason, validEntries);
    }

    private List<CacheEvictionEntry> wishlistEntries() {
        return List.of(
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_STATUS),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_STATUS_BATCH)
        );
    }

    private List<CacheEvictionEntry> staffCommissionEntries() {
        return List.of(
                CacheEvictionEntry.allEntries(CacheNames.STAFF_COMMISSION_SUMMARIES),
                CacheEvictionEntry.allEntries(CacheNames.STAFF_COMMISSION_DETAILS)
        );
    }

    private List<CacheEvictionEntry> consultationAttributionEntries() {
        return List.of(
                CacheEvictionEntry.allEntries(CacheNames.CONSULTATION_ATTRIBUTIONS),
                CacheEvictionEntry.allEntries(CacheNames.STAFF_COMMISSION_SUMMARIES),
                CacheEvictionEntry.allEntries(CacheNames.STAFF_COMMISSION_DETAILS)
        );
    }

    private String orderUserId(Order order) {
        return order == null || order.getUser() == null ? null : order.getUser().getId();
    }

    private String orderStaffId(Order order) {
        if (order == null) {
            return null;
        }
        if (order.getAssignedStaff() != null) {
            return order.getAssignedStaff().getId();
        }
        return order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
    }
}
