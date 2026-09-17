package com.example.workflow.workflow.handler;

import com.example.workflow.workflow.WorkflowTaskContext;
import com.example.workflow.workflow.WorkflowTaskHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ValidateGuestCheckoutWorkflowTaskHandler implements WorkflowTaskHandler {
    public static final String TASK_TYPE = "VALIDATE_GUEST_CHECKOUT";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+().\\-\\s]{8,30}$");
    private static final Set<String> PAYMENT_METHODS = Set.of("COD", "ONLINE");

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(WorkflowTaskContext context) {
        List<String> errors = new ArrayList<>();
        if (!context.isTrue("guestSessionResolved")) {
            errors.add("Guest session has not been resolved");
        }

        requireText(context, errors, "customerName", "Customer name is required");
        validateEmail(context, errors);
        validatePhone(context, errors);
        requireText(context, errors, "shippingAddress", "Shipping address is required");
        validateCart(context, errors);
        validatePaymentMethod(context, errors);

        context.setVariable("guestCheckoutValidationErrors", errors);
        context.setVariable("guestCheckoutValid", errors.isEmpty());
    }

    private void validateEmail(WorkflowTaskContext context, List<String> errors) {
        String email = context.getString("email");
        if (!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            errors.add("A valid guest email is required");
        }
    }

    private void validatePhone(WorkflowTaskContext context, List<String> errors) {
        String phone = context.getString("phone");
        if (!StringUtils.hasText(phone) || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            errors.add("A valid guest phone is required");
        }
    }

    private void validateCart(WorkflowTaskContext context, List<String> errors) {
        Object variantIds = context.getVariable("variantIds");
        boolean hasVariantIds = variantIds instanceof Collection<?> collection && !collection.isEmpty();
        Long cartItemCount = hasVariantIds ? null : context.getLong("cartItemCount");
        if (!hasVariantIds && (cartItemCount == null || cartItemCount <= 0)) {
            errors.add("Guest cart must contain at least one item");
        }
    }

    private void validatePaymentMethod(WorkflowTaskContext context, List<String> errors) {
        String paymentMethod = context.getString("paymentMethod");
        if (!StringUtils.hasText(paymentMethod)
                || !PAYMENT_METHODS.contains(paymentMethod.trim().toUpperCase(Locale.ROOT))) {
            errors.add("Payment method must be COD or ONLINE");
        }
    }

    private void requireText(WorkflowTaskContext context, List<String> errors, String variable, String message) {
        if (!StringUtils.hasText(context.getString(variable))) {
            errors.add(message);
        }
    }
}
