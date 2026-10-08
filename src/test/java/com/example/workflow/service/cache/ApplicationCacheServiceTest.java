package com.example.workflow.service.cache;

import com.example.workflow.cache.CacheKeys;
import com.example.workflow.cache.CacheNames;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.User;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.service.redis.DeferredCacheEvictionPublisher;
import com.example.workflow.service.redis.OptionalCacheService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApplicationCacheServiceTest {
    private final OptionalCacheService optionalCacheService = mock(OptionalCacheService.class);
    private final DeferredCacheEvictionPublisher cacheEvictionPublisher = mock(DeferredCacheEvictionPublisher.class);
    private final ApplicationCacheService applicationCacheService = new ApplicationCacheService(
            optionalCacheService,
            cacheEvictionPublisher
    );

    @Test
    void userCheckoutOnlyEvictsCartImmediately() {
        applicationCacheService.evictUserCheckoutCart("user-1");

        verify(optionalCacheService).evictAfterCommit(CacheNames.CARTS, "user-user-1");
        verifyNoInteractions(cacheEvictionPublisher);
    }

    @Test
    void orderCreatedEvictsOnlyAffectedUserReadModels() {
        Order order = order("user-1", null);
        order.setUserVoucher(new UserVoucher());

        applicationCacheService.evictOrderCreated(order);

        verify(optionalCacheService).evictByPrefixAfterCommit(
                CacheNames.USER_ORDERS,
                CacheKeys.userOrdersPrefix("user-1")
        );
        verify(optionalCacheService).evictAfterCommit(CacheNames.USER_VOUCHER_WALLET, "user-1");
        verify(optionalCacheService, never()).clearAfterCommit(CacheNames.PRODUCTS);
        verify(optionalCacheService, never()).clearAfterCommit(CacheNames.PRODUCT);
        verify(optionalCacheService, never()).clearAfterCommit(CacheNames.DASHBOARD_STATS);
        verifyNoInteractions(cacheEvictionPublisher);
    }

    @Test
    void orderCancelledEvictsOperationalCachesButNotCatalogOrAnalytics() {
        Order order = order("user-1", "staff-1");

        applicationCacheService.evictOrderCancelled(order, OrderStatus.PENDING_APPROVAL);

        verify(optionalCacheService).evictByPrefixAfterCommit(
                CacheNames.USER_ORDERS,
                CacheKeys.userOrdersPrefix("user-1")
        );
        verify(optionalCacheService).evictByPrefixAfterCommit(
                CacheNames.USER_CANCELLED_ORDERS,
                CacheKeys.userCancelledOrdersPrefix("user-1")
        );
        verify(optionalCacheService).evictByPrefixAfterCommit(
                CacheNames.MANAGER_PENDING_ORDERS,
                CacheKeys.managerPendingOrdersPrefix(OrderStatus.PENDING_APPROVAL)
        );
        verify(optionalCacheService).evictByPrefixAfterCommit(
                CacheNames.STAFF_ASSIGNED_ORDERS,
                CacheKeys.staffAssignedOrdersPrefix("staff-1")
        );
        verify(optionalCacheService, never()).clearAfterCommit(CacheNames.PRODUCTS);
        verify(optionalCacheService, never()).clearAfterCommit(CacheNames.PRODUCT);
        verify(optionalCacheService, never()).clearAfterCommit(CacheNames.DASHBOARD_STATS);
        verifyNoInteractions(cacheEvictionPublisher);
    }

    private Order order(String userId, String staffId) {
        User user = new User();
        user.setId(userId);

        Order order = new Order();
        order.setUser(user);
        if (staffId != null) {
            User staff = new User();
            staff.setId(staffId);
            order.setAssignedStaff(staff);
        }
        return order;
    }
}
