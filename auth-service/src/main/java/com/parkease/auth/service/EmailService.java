package com.parkease.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ParkEase — Reset Your Password");

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            message.setText(
                "Hi there,\n\n" +
                "We received a request to reset your ParkEase password.\n\n" +
                "Click the link below to set a new password:\n" +
                resetLink + "\n\n" +
                "This link expires in 15 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— The ParkEase Team"
            );

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }


    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to ParkEase!");
            message.setText(
                "Hi " + fullName + ",\n\n" +
                "Welcome to ParkEase! Your account has been created successfully.\n\n" +
                "You can now find and reserve parking spots near you.\n\n" +
                "— The ParkEase Team"
            );

            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}
