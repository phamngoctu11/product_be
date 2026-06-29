package com.example.workflow.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MomoPaymentRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void amountMustBePositive() {
        MomoPaymentRequest request = new MomoPaymentRequest();
        request.setOrderId("order-1");
        request.setAmount(0);

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getMessage().equals("Amount must be positive"));
    }

    @Test
    void orderIdIsRequired() {
        MomoPaymentRequest request = new MomoPaymentRequest();
        request.setAmount(1000);

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getMessage().equals("Order id is required"));
    }

    @Test
    void blankOrderIdIsRejected() {
        MomoPaymentRequest request = new MomoPaymentRequest();
        request.setOrderId(" ");
        request.setAmount(1000);

        var violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getMessage().equals("Order id is required"));
    }

    @Test
    void validRequestHasNoViolations() {
        MomoPaymentRequest request = new MomoPaymentRequest();
        request.setOrderId("order-1");
        request.setAmount(1000);

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
