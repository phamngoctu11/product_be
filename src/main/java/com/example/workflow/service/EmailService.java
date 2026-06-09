package com.example.workflow.service;

import com.example.workflow.dto.ReceiptMismatchDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendOrderConfirmationEmail(String toEmail, String customerName, Long orderId, Double totalPrice, String paymentMethod) {
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
            helper.setSubject("Xác nhận đơn hàng #" + orderId + " từ My App");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println(">>> Đã gửi Email Hóa đơn thành công tới: " + toEmail);
        } catch (MessagingException e) {
            System.err.println(">>> Lỗi khi gửi email: " + e.getMessage());
        }
    }

    public void sendOrderCancellationEmail(String toEmail, String customerName, Long orderId, String reason) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderId", orderId);
            context.setVariable("reason", reason);

            String htmlContent = templateEngine.process("order-cancelled", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Thông báo Hủy đơn hàng #" + orderId + " từ My App");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println(">>> Đã gửi Email HỦY ĐƠN thành công tới: " + toEmail);
        } catch (MessagingException e) {
            System.err.println(">>> Lỗi khi gửi email hủy đơn: " + e.getMessage());
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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmails.toArray(new String[0]));
            helper.setSubject("Khieu nai lech so luong don hang #" + orderId);
            helper.setText(buildReceiptComplaintHtml(orderId, customerName, customerEmail, note, mismatches), true);

            mailSender.send(message);
            System.out.println(">>> Sent receipt complaint email for order #" + orderId);
        } catch (MessagingException e) {
            System.err.println(">>> Error sending receipt complaint email: " + e.getMessage());
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
                    <h2>Khieu nai lech so luong don hang #%s</h2>
                    <p><b>Khach hang:</b> %s</p>
                    <p><b>Email khach hang:</b> %s</p>
                    <p><b>Ghi chu:</b> %s</p>
                    <table cellpadding="8" cellspacing="0" border="1" style="border-collapse: collapse; width: 100%%;">
                        <thead>
                            <tr>
                                <th>Variant ID</th>
                                <th>Variant</th>
                                <th>So luong dat</th>
                                <th>So luong xuat</th>
                                <th>So luong khach nhan</th>
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
