package com.scheduling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * Sends a booking confirmation email asynchronously.
     * Failures are logged but never propagate to break the booking flow.
     */
    @Async
    public void sendBookingConfirmation(String toEmail, String teamName, String date, String slotTime, String status) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail username not configured — skipping email to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Booking " + status + " — Optimized Project Scheduling System");
            message.setText(
                "Hello " + teamName + ",\n\n" +
                "Your slot booking has been processed.\n\n" +
                "Details:\n" +
                "  Team Name : " + teamName + "\n" +
                "  Date      : " + date + "\n" +
                "  Slot Time : " + slotTime + "\n" +
                "  Status    : " + status + "\n\n" +
                "Thank you for using the Optimized Project Scheduling System.\n" +
                "— Scheduling System"
            );
            mailSender.send(message);
            log.info("Booking confirmation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
