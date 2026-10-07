package com.project.englishlearning.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerificationEmailSender {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final boolean credentialsConfigured;

    public VerificationEmailSender(ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.from:}") String from,
            @Value("${spring.mail.password:}") String password) {
        this.sender = sender;
        this.from = from;
        this.credentialsConfigured = !password.isBlank();
    }

    public void send(String email, String code) {
        JavaMailSender mailer = sender.getIfAvailable();
        if (mailer == null || from.isBlank() || !credentialsConfigured) {
            throw new IllegalStateException("Email delivery is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[EngMaster AI] Mã xác nhận đăng ký tài khoản");
        message.setText("Xin chào,\n\nMã xác nhận đăng ký EngMaster AI của bạn là: " + code
            + "\n\nMã có hiệu lực trong 10 phút và chỉ dùng một lần. Không chia sẻ mã này với người khác."
            + "\nNếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.\n\nEngMaster AI");
        mailer.send(message);
    }
}
