package com.example.workflow.controller;

import com.example.workflow.dto.AssignOrderRequest;
import com.example.workflow.dto.ManagerReviewRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private void assertManagerWriteContract(Method method) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAuthority('MANAGER')");

        RequestHeader idempotencyHeader = method.getParameters()[1].getAnnotation(RequestHeader.class);
        assertThat(idempotencyHeader).isNotNull();
        assertThat(idempotencyHeader.value()).isEqualTo("Idempotency-Key");
        assertThat(idempotencyHeader.required()).isTrue();
    }
}
