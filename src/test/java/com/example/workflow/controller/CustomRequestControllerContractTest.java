package com.example.workflow.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class CustomRequestControllerContractTest {
    @Test
    void allCustomRequestEndpointsRequireUserAuthority() {
        PreAuthorize authorization = CustomRequestController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAuthority('USER')");
    }
}
