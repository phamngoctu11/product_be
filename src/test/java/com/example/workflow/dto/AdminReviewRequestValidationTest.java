package com.example.workflow.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminReviewRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectReviewRequiresCancelReason() {
        AdminReviewRequest request = new AdminReviewRequest(false, "");

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getMessage().equals("Cancel reason is required when rejecting an order"));
    }

    @Test
    void approvedReviewDoesNotRequireCancelReason() {
        AdminReviewRequest request = new AdminReviewRequest(true, null);

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
