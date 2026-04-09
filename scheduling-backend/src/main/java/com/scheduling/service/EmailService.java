package com.scheduling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private static final String FROM_EMAIL = "kumarganesh6092@gmail.com";

    private final JavaMailSender mailSender;

    /**
     * Sends a booking confirmation / status email asynchronously via Gmail SMTP.
     * Failures are logged but never propagate to break the booking flow.
     */
    @Async
    public void sendBookingConfirmation(String toEmail, String projectName, String date, String slotTime, String status) {
        log.info("📨 Preparing to send booking email to recipient: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(toEmail);
            message.setSubject("Project Demo Slot Booking Status");
            message.setText(buildBookingText(projectName, date, slotTime, status));
            mailSender.send(message);
            log.info("✅ Booking email sent successfully to {}", toEmail);
        } catch (MailException e) {
            log.error("❌ Failed to send booking email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error sending booking email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends a team registration confirmation email asynchronously via Gmail SMTP.
     * Failures are logged but never propagate to break the registration flow.
     */
    @Async
    public void sendRegistrationConfirmation(String toEmail, String projectName, String leaderName, int memberCount) {
        log.info("📨 Preparing to send registration email to recipient: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(toEmail);
            message.setSubject("Team Registration Confirmed – Optimized Project Scheduling System");
            message.setText(buildRegistrationText(projectName, leaderName, memberCount));
            mailSender.send(message);
            log.info("✅ Registration email sent successfully to {}", toEmail);
        } catch (MailException e) {
            log.error("❌ Failed to send registration email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error sending registration email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── Text Templates ─────────────────────────────────────────────────

    private String buildBookingText(String projectName, String date, String slotTime, String status) {
        return "Hello,\n\n"
                + "Your project demo slot booking status has been updated.\n\n"
                + "Project Name : " + projectName + "\n"
                + "Demo Date    : " + date + "\n"
                + "Time Slot    : " + slotTime + "\n"
                + "Status       : " + status.toUpperCase() + "\n\n"
                + "If you have any questions, please contact your course coordinator.\n\n"
                + "Thank you,\n"
                + "Optimized Project Scheduling System";
    }

    private String buildRegistrationText(String projectName, String leaderName, int memberCount) {
        return "Hello,\n\n"
                + "Your team has been successfully registered!\n\n"
                + "Project Name : " + projectName + "\n"
                + "Team Leader  : " + leaderName + "\n"
                + "Members      : " + memberCount + " member(s)\n\n"
                + "You can now log in to book a demo slot for your project presentation.\n\n"
                + "Thank you,\n"
                + "Optimized Project Scheduling System";
    }
}
