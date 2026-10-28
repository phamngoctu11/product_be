package com.example.workflow.service;

import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCancellationEmailRequestedEvent;
import com.example.workflow.event.payload.OrderConfirmationEmailRequestedEvent;
import com.example.workflow.event.payload.PasswordResetEmailRequestedEvent;
import com.example.workflow.event.payload.WorkflowEmailRequestedEvent;
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
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final DomainEventPublisher eventPublisher;

    public void sendOrderConfirmationEmail(String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod) {
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CONFIRMATION_EMAIL_REQUESTED,
                new OrderConfirmationEmailRequestedEvent(toEmail, customerName, orderId, totalPrice, paymentMethod)
        );
    }

    public void sendGuestOrderConfirmationEmail(
            String toEmail,
            String customerName,
            Long orderId,
            BigDecimal totalPrice,
            String paymentMethod,
            String orderAccessUrl,
            Integer productionDurationDays
    ) {
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CONFIRMATION_EMAIL_REQUESTED,
                new OrderConfirmationEmailRequestedEvent(
                        toEmail, customerName, orderId, totalPrice, paymentMethod,
                        orderAccessUrl, productionDurationDays, null, null
                )
        );
    }

    public void sendCustomOrderConfirmationEmail(
            String toEmail,
            String customerName,
            Long orderId,
            String customSpec,
            Integer quantity,
            String orderAccessUrl
    ) {
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CONFIRMATION_EMAIL_REQUESTED,
                new OrderConfirmationEmailRequestedEvent(
                        toEmail, customerName, orderId, null, null,
                        orderAccessUrl, null, customSpec, quantity
                )
        );
    }

    public void sendOrderAcceptedEmail(String toEmail, String customerName, Long orderId) {
        sendOrderAcceptedEmail(toEmail, customerName, orderId, null, null, null, null, null);
    }

    public void sendOrderAcceptedEmail(
            String toEmail,
            String customerName,
            Long orderId,
            String orderAccessUrl,
            String productSummary,
            BigDecimal finalPrice,
            Integer quantity,
            Integer productionDurationDays
    ) {
        StringBuilder html = new StringBuilder("<html><body><p>Xin chào ")
                .append(escapeHtml(customerName))
                .append(",</p><p>Đơn hàng #")
                .append(orderId)
                .append(" đã được chấp nhận và đang chờ bước tiếp theo.</p>");
        if (productSummary != null && !productSummary.isBlank()) {
            html.append("<p>Mẫu sản phẩm: ").append(escapeHtml(productSummary)).append("</p>");
        }
        if (finalPrice != null) {
            html.append("<p>Giá đã duyệt: ").append(finalPrice).append("</p>");
        }
        if (quantity != null) {
            html.append("<p>Số lượng: ").append(quantity).append("</p>");
        }
        if (productionDurationDays != null) {
            html.append("<p>Thời lượng dự kiến: ").append(productionDurationDays).append(" ngày</p>");
        }
        if (orderAccessUrl != null && !orderAccessUrl.isBlank()) {
            html.append("<p><a href=\"")
                    .append(escapeHtml(orderAccessUrl))
                    .append("\">Xem thông tin đơn hàng</a></p>");
        }
        html.append("</body></html>");
        eventPublisher.publishAfterCommit(
                EventTypes.WORKFLOW_EMAIL_REQUESTED,
                new WorkflowEmailRequestedEvent(
                        "ORDER_ACCEPTED",
                        toEmail,
                        "Đơn hàng #" + orderId + " đã được chấp nhận",
                        html.toString(),
                        orderId
                )
        );
    }

    public void sendProductionStartedEmail(
            String toEmail,
            String customerName,
            Long orderId,
            String orderAccessUrl
    ) {
        StringBuilder html = new StringBuilder("<html><body><p>Xin chào ")
                .append(escapeHtml(customerName))
                .append(",</p><p>Đơn hàng #")
                .append(orderId)
                .append(" đã bắt đầu sản xuất.</p>");
        if (orderAccessUrl != null && !orderAccessUrl.isBlank()) {
            html.append("<p><a href=\"")
                    .append(escapeHtml(orderAccessUrl))
                    .append("\">Xem thông tin đơn hàng</a></p>");
        }
        html.append("</body></html>");
        eventPublisher.publishAfterCommit(
                EventTypes.WORKFLOW_EMAIL_REQUESTED,
                new WorkflowEmailRequestedEvent(
                        "PRODUCTION_STARTED",
                        toEmail,
                        "Đơn hàng #" + orderId + " đã bắt đầu sản xuất",
                        html.toString(),
                        orderId
                )
        );
    }

    public void sendInitialShapeCheckpointEmail(
            String toEmail,
            String customerName,
            Long orderId,
            Long orderItemId,
            java.util.List<String> imageUrls,
            String note,
            String orderAccessUrl
    ) {
        StringBuilder html = new StringBuilder("<html><body><p>Xin chào ")
                .append(escapeHtml(customerName))
                .append(",</p><p>Đã có hình ảnh đầu tiên của sản phẩm #")
                .append(orderItemId)
                .append(" thuộc đơn hàng #")
                .append(orderId)
                .append(".</p>");
        if (note != null && !note.isBlank()) {
            html.append("<p>Ghi chú: ").append(escapeHtml(note)).append("</p>");
        }
        if (imageUrls != null && !imageUrls.isEmpty()) {
            html.append("<ul>");
            for (String imageUrl : imageUrls) {
                html.append("<li><a href=\"")
                        .append(escapeHtml(imageUrl))
                        .append("\">Xem hình ảnh tiến độ</a></li>");
            }
            html.append("</ul>");
        }
        if (orderAccessUrl != null && !orderAccessUrl.isBlank()) {
            html.append("<p><a href=\"")
                    .append(escapeHtml(orderAccessUrl))
                    .append("\">Xem thông tin đơn hàng</a></p>");
        }
        html.append("</body></html>");
        eventPublisher.publishAfterCommit(
                EventTypes.WORKFLOW_EMAIL_REQUESTED,
                new WorkflowEmailRequestedEvent(
                        "PRODUCTION_INITIAL_SHAPE",
                        toEmail,
                        "Hình ảnh tiến độ sản phẩm #" + orderItemId + " - đơn hàng #" + orderId,
                        html.toString(),
                        orderId
                )
        );
    }

    public void sendFinalProductCheckpointEmail(
            String toEmail,
            String customerName,
            Long orderId,
            Long orderItemId,
            int attemptNumber,
            java.util.List<String> imageUrls,
            String note,
            String orderAccessUrl
    ) {
        StringBuilder html = new StringBuilder("<html><body><p>Xin chào ")
                .append(escapeHtml(customerName))
                .append(",</p><p>Đã có hình ảnh sản phẩm hoàn thiện lần ")
                .append(attemptNumber)
                .append(" của sản phẩm #")
                .append(orderItemId)
                .append(" thuộc đơn hàng #")
                .append(orderId)
                .append(". Sản phẩm đang chờ kiểm tra chất lượng.</p>");
        if (note != null && !note.isBlank()) {
            html.append("<p>Ghi chú: ").append(escapeHtml(note)).append("</p>");
        }
        if (imageUrls != null && !imageUrls.isEmpty()) {
            html.append("<ul>");
            for (String imageUrl : imageUrls) {
                html.append("<li><a href=\"")
                        .append(escapeHtml(imageUrl))
                        .append("\">Xem hình ảnh sản phẩm hoàn thiện</a></li>");
            }
            html.append("</ul>");
        }
        if (orderAccessUrl != null && !orderAccessUrl.isBlank()) {
            html.append("<p><a href=\"")
                    .append(escapeHtml(orderAccessUrl))
                    .append("\">Xem thông tin đơn hàng</a></p>");
        }
        html.append("</body></html>");
        eventPublisher.publishAfterCommit(
                EventTypes.WORKFLOW_EMAIL_REQUESTED,
                new WorkflowEmailRequestedEvent(
                        "PRODUCTION_FINAL_PRODUCT",
                        toEmail,
                        "Hình ảnh sản phẩm hoàn thiện sản phẩm #" + orderItemId + " - đơn hàng #" + orderId,
                        html.toString(),
                        orderId
                )
        );
    }

    public void sendReadyToShipEmail(
            String toEmail,
            String customerName,
            Long orderId,
            String orderAccessUrl
    ) {
        StringBuilder html = new StringBuilder("<html><body><p>Xin chào ")
                .append(escapeHtml(customerName))
                .append(",</p><p>Đơn hàng #")
                .append(orderId)
                .append(" đã hoàn tất sản xuất và sẵn sàng bàn giao.</p>");
        if (orderAccessUrl != null && !orderAccessUrl.isBlank()) {
            html.append("<p><a href=\"")
                    .append(escapeHtml(orderAccessUrl))
                    .append("\">Xem thông tin đơn hàng</a></p>");
        }
        html.append("</body></html>");
        eventPublisher.publishAfterCommit(
                EventTypes.WORKFLOW_EMAIL_REQUESTED,
                new WorkflowEmailRequestedEvent(
                        "ORDER_READY_TO_SHIP",
                        toEmail,
                        "Đơn hàng #" + orderId + " đã sẵn sàng bàn giao",
                        html.toString(),
                        orderId
                )
        );
    }

    public void sendOrderConfirmationEmailNow(String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod) {
        try {
            sendOrderConfirmationEmailNowOrThrow(toEmail, customerName, orderId, totalPrice, paymentMethod);
        } catch (RuntimeException e) {
            log.warn("Optional order confirmation email failed for order {}: {}", orderId, e.getMessage());
        }
    }

    public void sendOrderConfirmationEmailNowOrThrow(String toEmail, String customerName, Long orderId, BigDecimal totalPrice, String paymentMethod) {
        sendOrderConfirmationEmailNowOrThrow(toEmail, customerName, orderId, totalPrice, paymentMethod, null, null);
    }

    public void sendOrderConfirmationEmailNowOrThrow(
            String toEmail,
            String customerName,
            Long orderId,
            BigDecimal totalPrice,
            String paymentMethod,
            String orderAccessUrl,
            Integer productionDurationDays
    ) {
        sendOrderConfirmationEmailNowOrThrow(
                toEmail, customerName, orderId, totalPrice, paymentMethod,
                orderAccessUrl, productionDurationDays, null, null
        );
    }

    public void sendOrderConfirmationEmailNowOrThrow(
            String toEmail,
            String customerName,
            Long orderId,
            BigDecimal totalPrice,
            String paymentMethod,
            String orderAccessUrl,
            Integer productionDurationDays,
            String customSpec,
            Integer quantity
    ) {
        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("orderId", orderId);
        context.setVariable("totalPrice", totalPrice);
        context.setVariable("paymentMethod", paymentMethod);
        context.setVariable("orderAccessUrl", orderAccessUrl);
        context.setVariable("productionDurationDays", productionDurationDays);
        context.setVariable("customSpec", customSpec);
        context.setVariable("quantity", quantity);

        String htmlContent = templateEngine.process("order-confirmation", context);
        sendHtmlEmailOrThrow(toEmail, "Xác nhận đơn hàng #" + orderId, htmlContent);
        log.info("Sent order confirmation email for order {} to {}", orderId, toEmail);
    }

    public void sendOrderCancellationEmail(String toEmail, String customerName, Long orderId, String reason) {
        eventPublisher.publishAfterCommit(
                EventTypes.ORDER_CANCELLATION_EMAIL_REQUESTED,
                new OrderCancellationEmailRequestedEvent(toEmail, customerName, orderId, reason)
        );
    }

    public void sendOrderCancellationEmailNow(String toEmail, String customerName, Long orderId, String reason) {
        try {
            sendOrderCancellationEmailNowOrThrow(toEmail, customerName, orderId, reason);
        } catch (RuntimeException e) {
            log.warn("Optional order cancellation email failed for order {}: {}", orderId, e.getMessage());
        }
    }

    public void sendOrderCancellationEmailNowOrThrow(String toEmail, String customerName, Long orderId, String reason) {
        Context context = new Context();
        context.setVariable("customerName", customerName);
        context.setVariable("orderId", orderId);
        context.setVariable("reason", reason);

        String htmlContent = templateEngine.process("order-cancelled", context);
        sendHtmlEmailOrThrow(toEmail, "Đơn hàng đã hủy #" + orderId, htmlContent);
        log.info("Sent order cancellation email for order {} to {}", orderId, toEmail);
    }

    public void sendPasswordResetEmail(String toEmail, String customerName, String resetLink, int expiresInMinutes) {
        eventPublisher.publishAfterCommit(
                EventTypes.PASSWORD_RESET_EMAIL_REQUESTED,
                new PasswordResetEmailRequestedEvent(toEmail, customerName, resetLink, expiresInMinutes)
        );
    }

    public void sendPasswordResetEmailNow(String toEmail, String customerName, String resetLink, int expiresInMinutes) {
        try {
            sendPasswordResetEmailNowOrThrow(toEmail, customerName, resetLink, expiresInMinutes);
        } catch (RuntimeException e) {
            log.warn("Optional password reset email failed for {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendPasswordResetEmailNowOrThrow(String toEmail, String customerName, String resetLink, int expiresInMinutes) {
        sendHtmlEmailOrThrow(
                toEmail,
                "Đặt lại mật khẩu tài khoản ShopVN",
                buildPasswordResetHtml(customerName, resetLink, expiresInMinutes)
        );
        log.info("Sent password reset email to {}", toEmail);
    }

    public void sendWorkflowEmailNowOrThrow(String toEmail, String subject, String htmlContent) {
        sendHtmlEmailOrThrow(toEmail, subject, htmlContent);
        log.info("Sent workflow email to {}", toEmail);
    }

    public void sendGuestWorkflowEmailNowOrThrow(
            String emailType,
            String toEmail,
            String customerName,
            Long orderId
    ) {
        String subject;
        String message;
        switch (emailType) {
            case "GUEST_READY_TO_SHIP" -> {
                subject = "Đơn hàng #" + orderId + " đã sẵn sàng bàn giao vận chuyển";
                message = "Đơn hàng thủ công của bạn đã sẵn sàng được bàn giao.";
            }
            case "GUEST_CHECKPOINT_INITIAL_SHAPE" -> {
                subject = "Điểm kiểm tra hình dạng ban đầu của đơn hàng #" + orderId;
                message = "Điểm kiểm tra hình dạng ban đầu của sản phẩm trong đơn hàng thủ công của bạn đã sẵn sàng.";
            }
            case "GUEST_CHECKPOINT_FINAL_PRODUCT" -> {
                subject = "Điểm kiểm tra sản phẩm hoàn thiện của đơn hàng #" + orderId;
                message = "Điểm kiểm tra sản phẩm hoàn thiện trong đơn hàng thủ công của bạn đã sẵn sàng.";
            }
            default -> throw new IllegalArgumentException("Loại email quy trình không được hỗ trợ '" + emailType + "'");
        }

        String htmlContent = "<html><body><p>Xin chào " + escapeHtml(customerName) + ",</p><p>"
                + escapeHtml(message) + "</p></body></html>";
        sendHtmlEmailOrThrow(toEmail, subject, htmlContent);
        log.info("Sent {} workflow email for order {} to {}", emailType, orderId, toEmail);
    }

    private void sendHtmlEmailOrThrow(String toEmail, String subject, String htmlContent) {
        sendHtmlEmailOrThrow(new String[]{toEmail}, subject, htmlContent);
    }

    private void sendHtmlEmailOrThrow(String[] toEmails, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmails);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            throw new IllegalStateException("Gửi email thất bại: " + e.getMessage(), e);
        }
    }

    private String buildPasswordResetHtml(String customerName, String resetLink, int expiresInMinutes) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222; line-height: 1.6;">
                    <h2>Đặt lại mật khẩu</h2>
                    <p>Xin chào %s,</p>
                    <p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                    <p>
                        <a href="%s" style="display:inline-block;padding:12px 18px;background:#4f46e5;color:#fff;text-decoration:none;border-radius:8px;font-weight:bold;">
                            Đặt lại mật khẩu
                        </a>
                    </p>
                    <p>Liên kết này có hiệu lực trong %s phút và chỉ sử dụng được một lần.</p>
                    <p>Nếu bạn không yêu cầu thao tác này, vui lòng bỏ qua email này.</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(customerName),
                escapeHtml(resetLink),
                expiresInMinutes
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
