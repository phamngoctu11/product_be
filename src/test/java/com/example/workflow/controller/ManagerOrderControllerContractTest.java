package com.example.workflow.controller;

import com.example.workflow.dto.AssignOrderRequest;
import com.example.workflow.dto.ManagerReviewRequest;
import com.example.workflow.dto.ReviewProductionCheckpointRequest;
import com.example.workflow.dto.SubmitProductionCheckpointRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ManagerOrderControllerContractTest {
    @Test
    void reviewAndAssignmentWritesRequireManagerAndIdempotencyKey() throws Exception {
        Method review = ManagerOrderController.class.getMethod(
                "reviewOrder", Long.class, String.class, ManagerReviewRequest.class
        );
        Method assignment = ManagerOrderController.class.getMethod(
                "assignStaffToOrder", Long.class, String.class, AssignOrderRequest.class
        );

        assertManagerWriteContract(review);
        assertManagerWriteContract(assignment);
    }

    @Test
    void assignmentQueueReadIsManagerOnly() throws Exception {
        Method queue = ManagerOrderController.class.getMethod("getAssignableOrders", Pageable.class);

        PreAuthorize authorization = queue.getAnnotation(PreAuthorize.class);
        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAuthority('MANAGER')");
    }

    @Test
    void productionCheckpointWritesRequireCorrectRoleAndIdempotencyKey() throws Exception {
        Method submit = StaffOrderController.class.getMethod(
                "submitProductionCheckpoint",
                Long.class,
                Long.class,
                String.class,
                SubmitProductionCheckpointRequest.class
        );
        Method review = ManagerOrderController.class.getMethod(
                "reviewProductionCheckpoint",
                Long.class,
                Long.class,
                String.class,
                ReviewProductionCheckpointRequest.class
        );

        assertWriteContract(submit, "hasAuthority('STAFF')", 2);
        assertWriteContract(review, "hasAuthority('MANAGER')", 2);
    }

    private void assertManagerWriteContract(Method method) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAuthority('MANAGER')");

        RequestHeader idempotencyHeader = method.getParameters()[1].getAnnotation(RequestHeader.class);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader.value()).isEqualTo("Idempotency-Key");
        assertThat(idempotencyHeader.required()).isTrue();
    }

    private void assertWriteContract(Method method, String authority, int headerParameterIndex) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo(authority);

        RequestHeader idempotencyHeader = method.getParameters()[headerParameterIndex]
                .getAnnotation(RequestHeader.class);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader.value()).isEqualTo("Idempotency-Key");
        assertThat(idempotencyHeader.required()).isTrue();
    }
}
