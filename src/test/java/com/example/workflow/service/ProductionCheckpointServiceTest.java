package com.example.workflow.service;

import com.example.workflow.dto.ProductionCheckpointDTO;
import com.example.workflow.dto.ReviewProductionCheckpointRequest;
import com.example.workflow.dto.ReviewProductionCheckpointResultDTO;
import com.example.workflow.dto.SubmitProductionCheckpointRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderAssignment;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.ProductionCheckpoint;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.ProductionMapper;
import com.example.workflow.nume.AssignmentSource;
import com.example.workflow.nume.AssignmentStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.ProductionCheckpointStage;
import com.example.workflow.nume.ProductionCheckpointStatus;
import com.example.workflow.nume.ProductionDecisionType;
import com.example.workflow.repository.OrderAssignmentRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.ProductionCheckpointRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.consistency.OrderTransitionService;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProductionCheckpointServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssignmentRepository assignmentRepository;
    @Mock private ProductionCheckpointRepository checkpointRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private DurableRequestExecutor durableRequests;
    @Mock private OrderTransitionService transitionService;
    @Mock private OrderStatusHistoryService historyService;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private OrderLookupTokenService orderLookupTokenService;

    private ProductionCheckpointService service;

    @BeforeEach
    void setUp() {
        ProductionMapper mapper = Mappers.getMapper(ProductionMapper.class);
        service = new ProductionCheckpointService(
                orderRepository,
                assignmentRepository,
                checkpointRepository,
                currentUserService,
                durableRequests,
                transitionService,
                historyService,
                eventPublisher,
                mapper,
                new ObjectMapper(),
                orderLookupTokenService
        );
        lenient().when(checkpointRepository.saveAndFlush(any(ProductionCheckpoint.class))).thenAnswer(invocation -> {
            ProductionCheckpoint checkpoint = invocation.getArgument(0);
            if (checkpoint.getId() == null) checkpoint.setId(900L + checkpoint.getAttemptNumber());
            if (checkpoint.getVersion() == null) checkpoint.setVersion(0L);
            return checkpoint;
        });
    }

    @Test
    void initialShapeIsImmediatelyPassedWithoutManagerReview() {
        Order order = producingOrder(item(501L), item(502L));
        User staff = user("staff-1");
        stubOrderAndAssignment(order, staff.getId());
        when(checkpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                501L, ProductionCheckpointStage.INITIAL_SHAPE)).thenReturn(Optional.empty());

        ProductionCheckpointDTO result = service.submitWithinTransaction(
                10L,
                501L,
                new SubmitProductionCheckpointRequest(
                        3L,
                        ProductionCheckpointStage.INITIAL_SHAPE,
                        List.of("https://cdn.example/initial-1.jpg", "https://cdn.example/initial-2.jpg"),
                        "Định hình xong"
                ),
                staff
        );

        assertEquals(ProductionCheckpointStatus.PASSED, result.status());
        assertFalse(result.managerReviewRequired());
        assertEquals(1, result.attemptNumber());
        assertEquals(2, result.images().size());
        assertEquals(OrderItemProductionStatus.FINAL_IN_PROGRESS, order.getItems().getFirst().getProductionStatus());
        assertEquals(OrderStatus.ORDER_CREATING, order.getStatus());
        verify(eventPublisher).publishAfterCommit(eq("PRODUCTION_CHECKPOINT_SUBMITTED"), any());
        verify(transitionService, never()).apply(anyLong(), anyLong(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void finalProductRequiresInitialShape() {
        Order order = producingOrder(item(501L));
        User staff = user("staff-1");
        stubOrderAndAssignment(order, staff.getId());
        when(checkpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                501L, ProductionCheckpointStage.INITIAL_SHAPE)).thenReturn(Optional.empty());

        AppException error = assertThrows(AppException.class, () -> service.submitWithinTransaction(
                10L,
                501L,
                new SubmitProductionCheckpointRequest(
                        3L,
                        ProductionCheckpointStage.FINAL_PRODUCT,
                        List.of("https://cdn.example/final.jpg"),
                        null
                ),
                staff
        ));

        assertEquals(ConstantErrorCode.PRODUCTION_FINAL_REQUIRES_INITIAL, error.getErrorCode());
        verify(eventPublisher, never()).publishAfterCommit(anyString(), any());
    }

    @Test
    void reworkKeepsOldAttemptAndNextSubmissionUsesNextAttemptNumber() {
        OrderItem item = item(501L);
        Order order = producingOrder(item);
        User manager = user("manager-1");
        User staff = user("staff-1");
        OrderAssignment assignment = stubOrderAndAssignment(order, staff.getId());
        ProductionCheckpoint initial = checkpoint(item, staff, 800L,
                ProductionCheckpointStage.INITIAL_SHAPE, 1, ProductionCheckpointStatus.PASSED);
        ProductionCheckpoint finalAttempt = checkpoint(item, staff, 801L,
                ProductionCheckpointStage.FINAL_PRODUCT, 1, ProductionCheckpointStatus.PENDING_REVIEW);
        finalAttempt.setManagerReviewRequired(true);
        finalAttempt.setVersion(2L);
        when(checkpointRepository.findByIdForUpdate(801L)).thenReturn(Optional.of(finalAttempt));
        when(checkpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                501L, ProductionCheckpointStage.FINAL_PRODUCT)).thenReturn(Optional.of(finalAttempt));
        when(checkpointRepository.findOrderItemIdsWithoutPassedFinal(10L)).thenReturn(List.of(501L));

        ReviewProductionCheckpointResultDTO review = service.reviewWithinTransaction(
                10L,
                801L,
                new ReviewProductionCheckpointRequest(
                        2L, ProductionDecisionType.REWORK_REQUIRED, "May chưa đều"
                ),
                manager
        );

        assertEquals(ProductionCheckpointStatus.REWORK_REQUIRED, finalAttempt.getStatus());
        assertEquals(ProductionDecisionType.REWORK_REQUIRED, finalAttempt.getDecision().getDecisionType());
        assertEquals("May chưa đều", finalAttempt.getDecision().getReason());
        assertEquals(OrderItemProductionStatus.FINAL_REWORK_REQUIRED, review.itemProductionStatus());
        assertFalse(review.assignmentReleased());
        assertEquals(AssignmentStatus.ACTIVE, assignment.getStatus());

        when(checkpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                501L, ProductionCheckpointStage.INITIAL_SHAPE)).thenReturn(Optional.of(initial));
        ProductionCheckpointDTO next = service.submitWithinTransaction(
                10L,
                501L,
                new SubmitProductionCheckpointRequest(
                        3L,
                        ProductionCheckpointStage.FINAL_PRODUCT,
                        List.of("https://cdn.example/final-rework.jpg"),
                        "Đã sửa"
                ),
                staff
        );

        assertEquals(2, next.attemptNumber());
        assertEquals(ProductionCheckpointStatus.PENDING_REVIEW, next.status());
        assertNotNull(finalAttempt.getDecision());
        assertEquals(801L, finalAttempt.getId());
    }

    @Test
    void passingOneOfManyItemsKeepsOrderCreatingAndStaffBusy() {
        OrderItem first = item(501L);
        Order order = producingOrder(first, item(502L));
        User manager = user("manager-1");
        User staff = user("staff-1");
        OrderAssignment assignment = stubOrderAndAssignment(order, staff.getId());
        ProductionCheckpoint checkpoint = checkpoint(first, staff, 801L,
                ProductionCheckpointStage.FINAL_PRODUCT, 1, ProductionCheckpointStatus.PENDING_REVIEW);
        checkpoint.setManagerReviewRequired(true);
        checkpoint.setVersion(0L);
        when(checkpointRepository.findByIdForUpdate(801L)).thenReturn(Optional.of(checkpoint));
        when(checkpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                501L, ProductionCheckpointStage.FINAL_PRODUCT)).thenReturn(Optional.of(checkpoint));
        when(checkpointRepository.findOrderItemIdsWithoutPassedFinal(10L)).thenReturn(List.of(502L));

        ReviewProductionCheckpointResultDTO result = service.reviewWithinTransaction(
                10L,
                801L,
                new ReviewProductionCheckpointRequest(0L, ProductionDecisionType.PASS, null),
                manager
        );

        assertEquals(OrderStatus.ORDER_CREATING, result.orderStatus());
        assertEquals(List.of(502L), result.remainingOrderItemIds());
        assertFalse(result.assignmentReleased());
        assertEquals(AssignmentStatus.ACTIVE, assignment.getStatus());
        verify(transitionService, never()).apply(anyLong(), anyLong(), any(), anyString(), anyString(), anyString(), any());
        verify(eventPublisher, never()).publishAfterCommit(eq("ORDER_READY_TO_SHIP"), any());
    }

    @Test
    void passingLastItemTransitionsOnceAndReleasesExactAssignment() {
        OrderItem item = item(501L);
        Order order = producingOrder(item);
        User manager = user("manager-1");
        User staff = user("staff-1");
        OrderAssignment assignment = stubOrderAndAssignment(order, staff.getId());
        ProductionCheckpoint checkpoint = checkpoint(item, staff, 801L,
                ProductionCheckpointStage.FINAL_PRODUCT, 1, ProductionCheckpointStatus.PENDING_REVIEW);
        checkpoint.setManagerReviewRequired(true);
        checkpoint.setVersion(0L);
        when(checkpointRepository.findByIdForUpdate(801L)).thenReturn(Optional.of(checkpoint));
        when(checkpointRepository.findTopByOrderItem_IdAndStageOrderByAttemptNumberDesc(
                501L, ProductionCheckpointStage.FINAL_PRODUCT)).thenReturn(Optional.of(checkpoint));
        when(checkpointRepository.findOrderItemIdsWithoutPassedFinal(10L)).thenReturn(List.of());
        when(transitionService.apply(
                eq(10L), eq(3L), eq(OrderStatus.READY_TO_SHIP), eq("manager-1"),
                anyString(), eq("final-kcs:10"), isNull()
        )).thenAnswer(invocation -> {
            order.setStatus(OrderStatus.READY_TO_SHIP);
            order.setVersion(4L);
            return order;
        });

        ReviewProductionCheckpointResultDTO result = service.reviewWithinTransaction(
                10L,
                801L,
                new ReviewProductionCheckpointRequest(0L, ProductionDecisionType.PASS, null),
                manager
        );

        assertEquals(OrderStatus.READY_TO_SHIP, result.orderStatus());
        assertTrue(result.assignmentReleased());
        assertEquals(AssignmentStatus.RELEASED, assignment.getStatus());
        assertEquals(10L, assignment.getOrderId());
        assertEquals("staff-1", assignment.getStaffId());
        assertEquals(null, assignment.getActiveOrderId());
        assertEquals(null, assignment.getActiveStaffId());
        verify(assignmentRepository).saveAndFlush(assignment);
        verify(historyService).record(order, OrderStatus.ORDER_CREATING, OrderStatus.READY_TO_SHIP, "manager-1");
        verify(eventPublisher).publishAfterCommit(eq("ORDER_READY_TO_SHIP"), any());
    }

    @Test
    void staffFromAnotherAssignmentCannotSubmitCheckpoint() {
        Order order = producingOrder(item(501L));
        stubOrderAndAssignment(order, "staff-1");

        AppException error = assertThrows(AppException.class, () -> service.submitWithinTransaction(
                10L,
                501L,
                new SubmitProductionCheckpointRequest(
                        3L,
                        ProductionCheckpointStage.INITIAL_SHAPE,
                        List.of("https://cdn.example/initial.jpg"),
                        null
                ),
                user("staff-2")
        ));

        assertEquals(ConstantErrorCode.PRODUCTION_CHECKPOINT_FORBIDDEN, error.getErrorCode());
    }

    @Test
    void guestProgressUsesSignedTokenAndHidesFinalProductAttempts() {
        OrderItem orderItem = item(501L);
        Order order = producingOrder(orderItem);
        User staff = user("staff-1");
        ProductionCheckpoint initial = checkpoint(
                orderItem, staff, 901L, ProductionCheckpointStage.INITIAL_SHAPE, 1,
                ProductionCheckpointStatus.PASSED
        );
        ProductionCheckpoint finalAttempt = checkpoint(
                orderItem, staff, 902L, ProductionCheckpointStage.FINAL_PRODUCT, 1,
                ProductionCheckpointStatus.PENDING_REVIEW
        );
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(checkpointRepository.findByOrderItem_IdOrderBySubmittedAtAsc(501L))
                .thenReturn(List.of(initial, finalAttempt));

        var result = service.getGuestProgress(10L, "signed-token");

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().checkpoints().size());
        assertEquals(ProductionCheckpointStage.INITIAL_SHAPE,
                result.getFirst().checkpoints().getFirst().stage());
        verify(orderLookupTokenService).authorize(order, "signed-token", OrderLookupTokenService.Scope.READ);
    }

    private OrderAssignment stubOrderAndAssignment(Order order, String staffId) {
        OrderAssignment assignment = activeAssignment(staffId);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findActiveByOrderIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private Order producingOrder(OrderItem... items) {
        Order order = new Order();
        order.setId(10L);
        order.setVersion(3L);
        order.setStatus(OrderStatus.ORDER_CREATING);
        order.setProductionStartedAt(LocalDateTime.of(2026, 10, 13, 9, 0));
        order.setItems(List.of(items));
        for (OrderItem item : items) item.setOrder(order);
        return order;
    }

    private OrderItem item(Long id) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setProductNameSnapshot("Item " + id);
        item.setQuantity(20);
        item.setProductionDurationDays(5);
        item.setProductionStatus(OrderItemProductionStatus.INITIAL_IN_PROGRESS);
        return item;
    }

    private ProductionCheckpoint checkpoint(
            OrderItem item,
            User staff,
            Long id,
            ProductionCheckpointStage stage,
            int attempt,
            ProductionCheckpointStatus status
    ) {
        ProductionCheckpoint checkpoint = new ProductionCheckpoint();
        checkpoint.setId(id);
        checkpoint.setOrderItem(item);
        checkpoint.setStage(stage);
        checkpoint.setAttemptNumber(attempt);
        checkpoint.setStatus(status);
        checkpoint.setSubmittedBy(staff);
        checkpoint.setSubmittedAt(LocalDateTime.of(2026, 10, 13, 10, 0));
        checkpoint.setVersion(0L);
        return checkpoint;
    }

    private OrderAssignment activeAssignment(String staffId) {
        OrderAssignment assignment = new OrderAssignment();
        assignment.occupy(10L, staffId, AssignmentSource.MANAGER, "manager-1",
                LocalDateTime.of(2026, 10, 12, 9, 0));
        assignment.setId(70L);
        return assignment;
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        user.setFirstname("Test");
        user.setLastname(id);
        user.setUsername(id);
        return user;
    }
}
