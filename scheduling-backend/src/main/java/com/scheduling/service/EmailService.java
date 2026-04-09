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

    private static final String FROM_EMAIL = "kumarganesh6092@gmail.com";
    private static final String BREVO_URL  = "https://api.brevo.com/v3/smtp/email";

    /**
     * Escapes a raw string so it is safe to embed inside a JSON string value.
     * Handles backslash, double-quote, and the common control characters
     * (newline, carriage-return, tab) that would otherwise break JSON parsing.
     */
    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw
                .replace("\\", "\\\\")   // backslash first
                .replace("\"", "\\\"")   // double-quote
                .replace("\n",  "\\n")   // newline
                .replace("\r",  "\\r")   // carriage return
                .replace("\t",  "\\t");  // tab
    }

    // 🔥 Generic email sender (Brevo API)
    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            // Build JSON body safely – no raw string formatting that could break JSON
            String body = "{"
                    + "\"sender\":{\"email\":\"" + escapeJson(FROM_EMAIL) + "\"},"
                    + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}],"
                    + "\"subject\":\"" + escapeJson(subject) + "\","
                    + "\"htmlContent\":\"" + escapeJson(htmlContent) + "\""
                    + "}";

            String apiKey = System.getenv("BREVO_API_KEY");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_URL))
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                log.info("✅ Email sent successfully to {}", toEmail);
            } else {
                log.error("❌ Brevo API error [status={}] for {}: {}",
                        response.statusCode(), toEmail, response.body());
            }

        } catch (Exception e) {
            log.error("❌ Email failed to {}: {}", toEmail, e.getMessage());
        }
    }

    // 📩 Booking Email
    @Async
    public void sendBookingConfirmation(String toEmail, String projectName,
                                        String date, String slotTime, String status) {
        String htmlContent =
                "<h2>Project Demo Slot Booking</h2>" +
                "<p>Your booking status has been updated.</p>" +
                "<ul>" +
                "<li><b>Project:</b> " + projectName + "</li>" +
                "<li><b>Date:</b> " + date + "</li>" +
                "<li><b>Time:</b> " + slotTime + "</li>" +
                "<li><b>Status:</b> " + status.toUpperCase() + "</li>" +
                "</ul>" +
                "<p>Thank you,<br>Project Scheduling System</p>";

        sendEmail(toEmail, "Project Slot Booking Status", htmlContent);
    }

    // 📩 Registration Email
    @Async
    public void sendRegistrationConfirmation(String toEmail, String projectName,
                                              String leaderName, int memberCount) {
        String htmlContent =
                "<h2>Team Registration Successful</h2>" +
                "<p>Your team has been registered successfully.</p>" +
                "<ul>" +
                "<li><b>Project:</b> " + projectName + "</li>" +
                "<li><b>Leader:</b> " + leaderName + "</li>" +
                "<li><b>Members:</b> " + memberCount + "</li>" +
                "</ul>" +
                "<p>You can now book your demo slot.</p>" +
                "<p>Thank you,<br>Project Scheduling System</p>";

        sendEmail(toEmail, "Team Registration Confirmed", htmlContent);
    }
}