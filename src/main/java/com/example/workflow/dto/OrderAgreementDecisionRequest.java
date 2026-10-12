package com.example.workflow.dto;

import com.example.workflow.nume.OrderAgreementDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderAgreementDecisionRequest(
        @NotNull Long orderVersion,
        @NotNull Integer agreementVersion,
        @NotNull OrderAgreementDecision decision,
        @Size(max = 1000) String reason
) {
}
