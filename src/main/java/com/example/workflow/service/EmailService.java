package com.example.workflow.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Công cụ xử lý file HTML Thymeleaf

    // Hàm gửi email xác nhận đơn hàng
    public void sendOrderConfirmationEmail(String toEmail, String customerName, Long orderId, Double totalPrice, String paymentMethod) {
        try {
            // 1. Tạo Context và nhét dữ liệu vào các biến chờ trong HTML
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderId", orderId);
            context.setVariable("totalPrice", totalPrice);
            context.setVariable("paymentMethod", paymentMethod);

            // 2. Trộn dữ liệu vào file "order-confirmation.html"
            String htmlContent = templateEngine.process("order-confirmation", context);

            // 3. Chuẩn bị phong bì thư (MimeMessage hỗ trợ gửi HTML)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail); // Gửi tới ai
            helper.setSubject("Xác nhận đơn hàng #" + orderId + " từ My App"); // Tiêu đề Email
            helper.setText(htmlContent, true); // true: Bật chế độ gửi nội dung là HTML

            // 4. Bấm nút gửi
            mailSender.send(message);
            System.out.println(">>> Đã gửi Email Hóa đơn thành công tới: " + toEmail);

        } catch (MessagingException e) {
            System.err.println(">>> Lỗi khi gửi email: " + e.getMessage());
        }
    }
    // Hàm gửi email thông báo Hủy đơn hàng
    public void sendOrderCancellationEmail(String toEmail, String customerName, Long orderId, String reason) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderId", orderId);
            context.setVariable("reason", reason);

            // Nạp file HTML màu đỏ vừa tạo
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
}