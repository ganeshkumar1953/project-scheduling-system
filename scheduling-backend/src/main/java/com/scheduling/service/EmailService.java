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
    private static final String RESEND_API_KEY = "re_SRnmzy6g_LbuB7a2KRugaYbTGFWGpHS5v";
    private static final String FROM_EMAIL = "onboarding@resend.dev";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Sends a booking confirmation email asynchronously via Resend API.
     * Failures are logged but never propagate to break the booking flow.
     */
    @Async
    public void sendBookingConfirmation(String toEmail, String projectName, String date, String slotTime, String status) {
        try {
            String htmlBody = buildHtmlBody(projectName, date, slotTime, status);

            // Escape JSON special characters in the HTML body
            String escapedHtml = htmlBody
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String jsonPayload = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    FROM_EMAIL,
                    toEmail,
                    "Project Demo Slot Booking Status",
                    escapedHtml
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + RESEND_API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode == 200 || statusCode == 201) {
                log.info("✅ Email sent successfully to {} via Resend API. Response: {}", toEmail, response.body());
            } else {
                log.error("❌ Resend API returned status {}: {}", statusCode, response.body());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send email to {} via Resend API: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Builds a styled HTML email body for booking notifications.
     */
    private String buildHtmlBody(String projectName, String date, String slotTime, String status) {
        String statusColor;
        switch (status.toUpperCase()) {
            case "CONFIRMED":
                statusColor = "#28a745";
                break;
            case "WAITLISTED":
                statusColor = "#ffc107";
                break;
            case "CANCELLED":
                statusColor = "#dc3545";
                break;
            case "RESCHEDULED":
                statusColor = "#17a2b8";
                break;
            default:
                statusColor = "#6c757d";
                break;
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
                + escapeHtml(status.toUpperCase()) + "</span></td></tr>"
                + "</table>"
                + "</div>"
                + "<div style=\"background: #f8f9fa; padding: 20px; border-radius: 0 0 12px 12px; border: 1px solid #e0e0e0; border-top: none; text-align: center;\">"
                + "<p style=\"color: #888; font-size: 13px; margin: 0;\">Thank you for using the Optimized Project Scheduling System.</p>"
                + "</div>"
                + "</div>";
    }

    /**
     * Basic HTML escaping to prevent XSS in email content.
     */
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
