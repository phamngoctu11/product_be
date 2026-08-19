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

    public void evictGuestCheckout(String guestSessionId) {
        if (StringUtils.hasText(guestSessionId)) {
            evictCartChanged("guest-" + guestSessionId.trim());
        }
        publishDeferredCacheEvictions(
                "guest checkout",
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCT)
        );
    }

    public void evictUserCheckout(String userId, Long userVoucherId) {
        evictUserCartChanged(userId);
        evictUserOrdersAfterCommit(userId);
        if (userVoucherId != null) {
            evictUserVoucherWalletAfterCommit(userId);
        }
        publishDeferredCacheEvictions(
                "user checkout",
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCT),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS)
        );
    }

    public void evictWarehouseClaimed(Order order, String staffId) {
        optionalCacheService.clearAfterCommit(CacheNames.WAREHOUSE_PENDING_ORDERS);
        evictStaffAssignedOrdersAfterCommit(staffId);
        publishDeferredCacheEvictions(
                "warehouse order claimed",
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                userOrdersEntry(order)
        );
    }

    public void evictStaffAssigned(Order order, OrderStatus oldStatus, String previousStaffId, String assignedStaffId) {
        if (oldStatus == OrderStatus.PENDING_WAREHOUSE) {
            optionalCacheService.clearAfterCommit(CacheNames.WAREHOUSE_PENDING_ORDERS);
            evictManagerPendingOrdersAfterCommit(OrderStatus.PENDING_WAREHOUSE);
        }
        publishDeferredCacheEvictions(
                "staff assigned to order",
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                staffAssignedOrdersEntry(previousStaffId),
                staffAssignedOrdersEntry(assignedStaffId),
                userOrdersEntry(order)
        );
    }

    public void evictManagerReviewed(Order order, boolean approved, String assignedStaffId) {
        evictManagerPendingOrdersAfterCommit(OrderStatus.PENDING_APPROVAL);
        publishDeferredCacheEvictions(
                "manager reviewed order",
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                approved && !StringUtils.hasText(assignedStaffId)
                        ? CacheEvictionEntry.allEntries(CacheNames.WAREHOUSE_PENDING_ORDERS)
                        : null,
                approved && StringUtils.hasText(assignedStaffId)
                        ? staffAssignedOrdersEntry(assignedStaffId)
                        : null,
                userOrdersEntry(order),
                approved ? null : userCancelledOrdersEntry(order),
                approved ? null : guestVoucherTemplatesEntry(order),
                approved ? null : voucherTemplatesEntryForGuestVoucher(order)
        );
    }

    public void evictStaffExported(Order order, String staffId) {
        evictStaffAssignedOrdersAfterCommit(staffId);
        publishDeferredCacheEvictions(
                "staff exported order",
                CacheEvictionEntry.prefix(CacheNames.MANAGER_PENDING_ORDERS, CacheKeys.managerPendingOrdersPrefix(OrderStatus.PENDING_KCS)),
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                userOrdersEntry(order)
        );
    }

    public void evictManagerKcsChecked(Order order) {
        evictManagerPendingOrdersAfterCommit(OrderStatus.PENDING_KCS);
        publishDeferredCacheEvictions(
                "manager KCS checked order",
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCT),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS),
                staffAssignedOrdersEntry(orderStaffId(order)),
                userOrdersEntry(order)
        );
    }

    public void evictCustomerReceiptConfirmed(Order order, String userId) {
        evictUserOrdersAfterCommit(userId);
        evictUserStateAfterCommit(userId);
        publishDeferredCacheEvictions(
                "customer confirmed receipt",
                staffAssignedOrdersEntry(orderStaffId(order)),
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                CacheEvictionEntry.allEntries(CacheNames.BEST_SELLING_PRODUCTS)
        );
    }

    public void evictCustomerCancelled(Order order, OrderStatus oldStatus, String userId) {
        evictUserOrdersAfterCommit(userId);
        evictUserCancelledOrdersAfterCommit(userId);
        evictUserVoucherWalletAfterCommit(userId);
        evictUserStateAfterCommit(userId);
        publishDeferredCacheEvictions(
                "order cancelled",
                CacheEvictionEntry.prefix(CacheNames.MANAGER_PENDING_ORDERS, CacheKeys.managerPendingOrdersPrefix(oldStatus)),
                CacheEvictionEntry.allEntries(CacheNames.WAREHOUSE_PENDING_ORDERS),
                staffAssignedOrdersEntry(orderStaffId(order)),
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCT),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS)
        );
    }

    public void evictCamundaOrderCancelled(Order order, OrderStatus oldStatus) {
        publishDeferredCacheEvictions("camunda order cancelled", orderCancellationEntries(order, oldStatus));
    }

    public void evictPendingPaymentReservationTimeout(Collection<Order> expiredOrders) {
        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return;
        }
        List<CacheEvictionEntry> entries = new ArrayList<>();
        for (Order order : expiredOrders) {
            entries.addAll(orderCancellationEntries(order, OrderStatus.PENDING_PAYMENT));
        }
        publishDeferredCacheEvictions("pending payment reservation timeout", entries);
    }

    public void evictMomoCallbackProcessed(Order order) {
        String userId = orderUserId(order);
        evictUserOrdersAfterCommit(userId);
        if (order != null && order.getStatus() == OrderStatus.CANCELLED) {
            evictUserCancelledOrdersAfterCommit(userId);
            evictUserVoucherWalletAfterCommit(userId);
        }
        publishDeferredCacheEvictions(
                "momo callback processed",
                order != null && order.getStatus() == OrderStatus.PENDING_APPROVAL
                        ? CacheEvictionEntry.prefix(CacheNames.MANAGER_PENDING_ORDERS, CacheKeys.managerPendingOrdersPrefix(OrderStatus.PENDING_APPROVAL))
                        : null,
                CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS)
        );
    }

    public void evictProductCreated() {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.clearAfterCommit(CacheNames.BEST_SELLING_PRODUCTS);
        publishDeferredCacheEvictions("product created", wishlistEntries());
    }

    public void evictProductUpdated(Long productId) {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.clearAfterCommit(CacheNames.BEST_SELLING_PRODUCTS);
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
        optionalCacheService.clearAfterCommit(CacheNames.BEST_SELLING_PRODUCTS);
        optionalCacheService.evictAfterCommit(CacheNames.PRODUCT, productId);
        publishDeferredCacheEvictions("product variant added", wishlistEntries());
    }

    public void evictStockImported() {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.clearAfterCommit(CacheNames.BEST_SELLING_PRODUCTS);
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCT);
        publishDeferredCacheEvictions("stock imported", wishlistEntries());
    }

    public void evictProductDeleted(Long productId) {
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.clearAfterCommit(CacheNames.BEST_SELLING_PRODUCTS);
        optionalCacheService.evictAfterCommit(CacheNames.PRODUCT, productId);
        publishDeferredCacheEvictions("product deleted", wishlistEntries());
    }

    public void evictInventoryDeducted() {
        publishDeferredCacheEvictions(
                "Camunda inventory deducted",
                CacheEvictionEntry.allEntries(CacheNames.PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.PRODUCT),
                CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS),
                CacheEvictionEntry.allEntries(CacheNames.BEST_SELLING_PRODUCTS)
        );
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
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCT);
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
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCT);
        optionalCacheService.clearAfterCommit(CacheNames.PRODUCTS);
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

    private List<CacheEvictionEntry> orderCancellationEntries(Order order, OrderStatus oldStatus) {
        List<CacheEvictionEntry> entries = new ArrayList<>();
        String userId = orderUserId(order);
        if (StringUtils.hasText(userId)) {
            entries.add(CacheEvictionEntry.prefix(CacheNames.USER_ORDERS, CacheKeys.userOrdersPrefix(userId)));
            entries.add(CacheEvictionEntry.prefix(CacheNames.USER_CANCELLED_ORDERS, CacheKeys.userCancelledOrdersPrefix(userId)));
            entries.add(CacheEvictionEntry.key(CacheNames.USER_VOUCHER_WALLET, userId));
        }
        entries.add(CacheEvictionEntry.prefix(CacheNames.MANAGER_PENDING_ORDERS, CacheKeys.managerPendingOrdersPrefix(oldStatus)));
        if (oldStatus == OrderStatus.PENDING_WAREHOUSE) {
            entries.add(CacheEvictionEntry.allEntries(CacheNames.WAREHOUSE_PENDING_ORDERS));
        }
        entries.add(staffAssignedOrdersEntry(orderStaffId(order)));
        entries.add(CacheEvictionEntry.allEntries(CacheNames.DASHBOARD_STATS));
        entries.add(CacheEvictionEntry.allEntries(CacheNames.PRODUCTS));
        entries.add(CacheEvictionEntry.allEntries(CacheNames.PRODUCT));
        entries.add(CacheEvictionEntry.allEntries(CacheNames.WISHLIST_PRODUCTS));
        if (order != null && order.getGuestVoucherTemplate() != null) {
            entries.add(CacheEvictionEntry.allEntries(CacheNames.GUEST_VOUCHER_TEMPLATES));
            entries.add(CacheEvictionEntry.allEntries(CacheNames.VOUCHER_TEMPLATES));
        }
        return entries;
    }

    private CacheEvictionEntry userOrdersEntry(Order order) {
        String userId = orderUserId(order);
        return StringUtils.hasText(userId)
                ? CacheEvictionEntry.prefix(CacheNames.USER_ORDERS, CacheKeys.userOrdersPrefix(userId))
                : null;
    }

    private CacheEvictionEntry userCancelledOrdersEntry(Order order) {
        String userId = orderUserId(order);
        return StringUtils.hasText(userId)
                ? CacheEvictionEntry.prefix(CacheNames.USER_CANCELLED_ORDERS, CacheKeys.userCancelledOrdersPrefix(userId))
                : null;
    }

    private CacheEvictionEntry staffAssignedOrdersEntry(String staffId) {
        return StringUtils.hasText(staffId)
                ? CacheEvictionEntry.prefix(CacheNames.STAFF_ASSIGNED_ORDERS, CacheKeys.staffAssignedOrdersPrefix(staffId))
                : null;
    }

    private CacheEvictionEntry guestVoucherTemplatesEntry(Order order) {
        return order != null && order.getUser() == null && order.getGuestVoucherTemplate() != null
                ? CacheEvictionEntry.allEntries(CacheNames.GUEST_VOUCHER_TEMPLATES)
                : null;
    }

    private CacheEvictionEntry voucherTemplatesEntryForGuestVoucher(Order order) {
        return order != null && order.getUser() == null && order.getGuestVoucherTemplate() != null
                ? CacheEvictionEntry.allEntries(CacheNames.VOUCHER_TEMPLATES)
                : null;
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
        return order == null || order.getWarehouseStaff() == null ? null : order.getWarehouseStaff().getId();
    }
}
