package com.example.workflow.service;

import com.example.workflow.dto.ReceiptMismatchDTO;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancellationEmailRequestedEvent;
import com.example.workflow.event.payload.OrderConfirmationEmailRequestedEvent;
import com.example.workflow.event.payload.ReceiptComplaintEmailRequestedEvent;
import com.example.workflow.service.redis.DomainEventPublisher;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final DomainEventPublisher eventPublisher;

    public void sendOrderConfirmationEmail(String toEmail, String customerName, Long orderId, Double totalPrice, String paymentMethod) {
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CONFIRMATION_EMAIL_REQUESTED,
                new OrderConfirmationEmailRequestedEvent(toEmail, customerName, orderId, totalPrice, paymentMethod)
        );
    }

    public void sendOrderConfirmationEmailNow(String toEmail, String customerName, Long orderId, Double totalPrice, String paymentMethod) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderId", orderId);
            context.setVariable("totalPrice", totalPrice);
            context.setVariable("paymentMethod", paymentMethod);

            String htmlContent = templateEngine.process("order-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Order confirmation #" + orderId);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent order confirmation email for order {} to {}", orderId, toEmail);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Optional order confirmation email failed for order {}: {}", orderId, e.getMessage());
        }
    }

    public void sendOrderCancellationEmail(String toEmail, String customerName, Long orderId, String reason) {
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLATION_EMAIL_REQUESTED,
                new OrderCancellationEmailRequestedEvent(toEmail, customerName, orderId, reason)
        );
    }

    public void sendOrderCancellationEmailNow(String toEmail, String customerName, Long orderId, String reason) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderId", orderId);
            context.setVariable("reason", reason);

            String htmlContent = templateEngine.process("order-cancelled", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Order cancelled #" + orderId);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent order cancellation email for order {} to {}", orderId, toEmail);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Optional order cancellation email failed for order {}: {}", orderId, e.getMessage());
        }
    }

    public void sendReceiptComplaintEmail(
            List<String> toEmails,
            Long orderId,
            String customerName,
            String customerEmail,
            String note,
            List<ReceiptMismatchDTO> mismatches
    ) {
        if (toEmails == null || toEmails.isEmpty()) {
            return;
        }
        eventPublisher.publishAfterCommit(
                EventTypes.RECEIPT_COMPLAINT_EMAIL_REQUESTED,
                new ReceiptComplaintEmailRequestedEvent(toEmails, orderId, customerName, customerEmail, note, mismatches)
        );
    }

    public void sendReceiptComplaintEmailNow(
            List<String> toEmails,
            Long orderId,
            String customerName,
            String customerEmail,
            String note,
            List<ReceiptMismatchDTO> mismatches
    ) {
        if (toEmails == null || toEmails.isEmpty()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmails.toArray(new String[0]));
            helper.setSubject("Receipt quantity complaint for order #" + orderId);
            helper.setText(buildReceiptComplaintHtml(orderId, customerName, customerEmail, note, mismatches), true);

            mailSender.send(message);
            log.info("Sent receipt complaint email for order {}", orderId);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Optional receipt complaint email failed for order {}: {}", orderId, e.getMessage());
        }
    }

    private String buildReceiptComplaintHtml(
            Long orderId,
            String customerName,
            String customerEmail,
            String note,
            List<ReceiptMismatchDTO> mismatches
    ) {
        StringBuilder rows = new StringBuilder();
        for (ReceiptMismatchDTO mismatch : mismatches) {
            rows.append("<tr>")
                    .append("<td>").append(escapeHtml(String.valueOf(mismatch.getVariantId()))).append("</td>")
                    .append("<td>").append(escapeHtml(mismatch.getVariantName())).append("</td>")
                    .append("<td style=\"text-align:right\">").append(mismatch.getOrderedQuantity()).append("</td>")
                    .append("<td style=\"text-align:right\">").append(mismatch.getExportedQuantity()).append("</td>")
                    .append("<td style=\"text-align:right\">").append(mismatch.getReceivedQuantity()).append("</td>")
                    .append("</tr>");
        }

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                    <h2>Receipt quantity complaint for order #%s</h2>
                    <p><b>Customer:</b> %s</p>
                    <p><b>Customer email:</b> %s</p>
                    <p><b>Note:</b> %s</p>
                    <table cellpadding="8" cellspacing="0" border="1" style="border-collapse: collapse; width: 100%%;">
                        <thead>
                            <tr>
                                <th>Variant ID</th>
                                <th>Variant</th>
                                <th>Ordered quantity</th>
                                <th>Exported quantity</th>
                                <th>Received quantity</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </body>
                </html>
                """.formatted(
                orderId,
                escapeHtml(customerName),
                escapeHtml(customerEmail),
                escapeHtml(note),
                rows
        );
    }

    private String escapeHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
