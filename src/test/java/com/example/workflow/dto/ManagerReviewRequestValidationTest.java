package com.example.workflow.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManagerReviewRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectionRequiresReason() {
        ManagerReviewRequest request = new ManagerReviewRequest(0L, false, "", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void approvalMayIncludeStaff() {
        ManagerReviewRequest request = new ManagerReviewRequest(0L, true, null, "staff-1");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectionCannotIncludeStaff() {
        ManagerReviewRequest request = new ManagerReviewRequest(0L, false, "Không thể thực hiện", "staff-1");

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
