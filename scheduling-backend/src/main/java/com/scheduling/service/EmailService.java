package com.scheduling.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String FROM_EMAIL = "Ganesh <kumarganesh6092@gmail.com>";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Reads Resend API key from environment variable RESEND_API_KEY.
     * Returns null if not configured — emails will be skipped with a warning.
     */
    private String getApiKey() {
        return System.getenv("RESEND_API_KEY");
    }

    /**
     * Sends a booking confirmation / status email asynchronously via Resend API.
     * Failures are logged but never propagate to break the booking flow.
     */
    @Async
    public void sendBookingConfirmation(String toEmail, String projectName, String date, String slotTime, String status) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ RESEND_API_KEY not configured — skipping email to {}", toEmail);
            return;
        }
        log.info("📨 Preparing to send booking email to recipient: {}", toEmail);
        try {
            String htmlBody = buildBookingHtml(projectName, date, slotTime, status);
            sendEmail(apiKey, toEmail, "Project Demo Slot Booking Status", htmlBody);
        } catch (Exception e) {
            log.error("❌ Failed to send booking email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends a team registration confirmation email asynchronously via Resend API.
     * Failures are logged but never propagate to break the registration flow.
     */
    @Async
    public void sendRegistrationConfirmation(String toEmail, String projectName, String leaderName, int memberCount) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ RESEND_API_KEY not configured — skipping registration email to {}", toEmail);
            return;
        }
        log.info("📨 Preparing to send registration email to recipient: {}", toEmail);
        try {
            String htmlBody = buildRegistrationHtml(projectName, leaderName, memberCount);
            sendEmail(apiKey, toEmail, "Team Registration Confirmed", htmlBody);
        } catch (Exception e) {
            log.error("❌ Failed to send registration email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Core method — sends an email via Resend REST API.
     */
    private void sendEmail(String apiKey, String toEmail, String subject, String htmlBody) throws Exception {
        String escapedHtml = htmlBody
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        String escapedSubject = subject.replace("\"", "\\\"");

        String jsonPayload = String.format(
                "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                FROM_EMAIL,
                toEmail,
                escapedSubject,
                escapedHtml
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        log.info("📧 Sending email to {} | Subject: {}", toEmail, subject);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode == 200 || statusCode == 201) {
            log.info("✅ Email sent successfully to {} | Response: {}", toEmail, response.body());
        } else {
            log.error("❌ Resend API returned status {}: {}", statusCode, response.body());
        }
    }

    // ── HTML Templates ─────────────────────────────────────────────────

    private String buildBookingHtml(String projectName, String date, String slotTime, String status) {
        String statusColor;
        String upperStatus = status.toUpperCase();
        if (upperStatus.contains("CONFIRMED")) {
            statusColor = "#28a745";
        } else if (upperStatus.contains("WAITLISTED")) {
            statusColor = "#ffc107";
        } else if (upperStatus.contains("CANCELLED")) {
            statusColor = "#dc3545";
        } else if (upperStatus.contains("RESCHEDULED")) {
            statusColor = "#17a2b8";
        } else {
            statusColor = "#6c757d";
        }

        return "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">"
                + "<div style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 12px 12px 0 0; text-align: center;\">"
                + "<h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">📅 Project Demo Slot Booking</h1>"
                + "<p style=\"color: #e0d4f7; margin-top: 8px;\">Optimized Project Scheduling System</p>"
                + "</div>"
                + "<div style=\"background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none;\">"
                + "<table style=\"width: 100%; border-collapse: collapse;\">"
                + "<tr><td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #555; font-weight: 600;\">Project Name</td>"
                + "<td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #333;\">" + escapeHtml(projectName) + "</td></tr>"
                + "<tr><td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #555; font-weight: 600;\">Date</td>"
                + "<td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #333;\">" + escapeHtml(date) + "</td></tr>"
                + "<tr><td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #555; font-weight: 600;\">Time Slot</td>"
                + "<td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #333;\">" + escapeHtml(slotTime) + "</td></tr>"
                + "<tr><td style=\"padding: 12px; color: #555; font-weight: 600;\">Status</td>"
                + "<td style=\"padding: 12px;\"><span style=\"background: " + statusColor + "; color: #fff; padding: 6px 16px; border-radius: 20px; font-weight: 600; font-size: 14px;\">"
                + escapeHtml(upperStatus) + "</span></td></tr>"
                + "</table>"
                + "</div>"
                + "<div style=\"background: #f8f9fa; padding: 20px; border-radius: 0 0 12px 12px; border: 1px solid #e0e0e0; border-top: none; text-align: center;\">"
                + "<p style=\"color: #888; font-size: 13px; margin: 0;\">Thank you for using the Optimized Project Scheduling System.</p>"
                + "</div>"
                + "</div>";
    }

    private String buildRegistrationHtml(String projectName, String leaderName, int memberCount) {
        return "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">"
                + "<div style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 12px 12px 0 0; text-align: center;\">"
                + "<h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">🎉 Team Registration Confirmed</h1>"
                + "<p style=\"color: #e0d4f7; margin-top: 8px;\">Optimized Project Scheduling System</p>"
                + "</div>"
                + "<div style=\"background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none;\">"
                + "<p style=\"color: #333; font-size: 16px;\">Your team has been successfully registered!</p>"
                + "<table style=\"width: 100%; border-collapse: collapse; margin-top: 16px;\">"
                + "<tr><td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #555; font-weight: 600;\">Project Name</td>"
                + "<td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #333;\">" + escapeHtml(projectName) + "</td></tr>"
                + "<tr><td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #555; font-weight: 600;\">Team Leader</td>"
                + "<td style=\"padding: 12px; border-bottom: 1px solid #f0f0f0; color: #333;\">" + escapeHtml(leaderName) + "</td></tr>"
                + "<tr><td style=\"padding: 12px; color: #555; font-weight: 600;\">Members</td>"
                + "<td style=\"padding: 12px; color: #333;\">" + memberCount + " member(s)</td></tr>"
                + "</table>"
                + "<p style=\"color: #555; font-size: 14px; margin-top: 20px;\">You can now log in to book demo slots for your project presentation.</p>"
                + "</div>"
                + "<div style=\"background: #f8f9fa; padding: 20px; border-radius: 0 0 12px 12px; border: 1px solid #e0e0e0; border-top: none; text-align: center;\">"
                + "<p style=\"color: #888; font-size: 13px; margin: 0;\">Thank you for using the Optimized Project Scheduling System.</p>"
                + "</div>"
                + "</div>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
