package com.example.workflow.workflow.handler;

import com.example.workflow.service.CustomAgreementTimeoutService;
import com.example.workflow.service.PaymentTimeoutService;
import com.example.workflow.workflow.WorkflowTaskContext;
import com.example.workflow.workflow.WorkflowTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CancelExpiredOrderWorkflowTaskHandler implements WorkflowTaskHandler {
    public static final String CUSTOM_TASK_TYPE = "CANCEL_CUSTOM_AGREEMENT_TIMEOUT";
    public static final String PAYMENT_TASK_TYPE = "CANCEL_PAYMENT_TIMEOUT";

    private final CustomAgreementTimeoutService customTimeoutService;
    private final PaymentTimeoutService paymentTimeoutService;

    @Override
    public String taskType() {
        return CUSTOM_TASK_TYPE;
    }

    @Override
    public List<String> taskTypes() {
        return List.of(CUSTOM_TASK_TYPE, PAYMENT_TASK_TYPE);
    }

    @Override
    public void handle(WorkflowTaskContext context) {
        String taskType = context.requireString("taskType");
        boolean cancelled = switch (taskType) {
            case CUSTOM_TASK_TYPE -> customTimeoutService.cancelIfStillExpired(context.requireLong("orderId"));
            case PAYMENT_TASK_TYPE -> paymentTimeoutService.expireIfStillDue(context.requireLong("paymentAttemptId"));
            default -> throw new IllegalArgumentException("Unsupported timeout taskType '" + taskType + "'");
        };
        context.setVariable("timeoutCancelled", cancelled);
    }
}
