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

    // 🔥 Generic email sender (Brevo API)
    @Async
    public void sendEmail(String toEmail, String subject, String content) {
        try {
            String body = """
            {
              "sender": {"name":"Project Scheduling System","email":"%s"},
              "to": [{"email":"%s"}],
              "subject":"%s",
              "htmlContent":"%s"
            }
            """.formatted(FROM_EMAIL, toEmail, subject, content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", System.getenv("BREVO_API_KEY"))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            log.info("📧 Brevo response: {}", response.body());
            log.info("✅ Email sent to {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Email failed to {}: {}", toEmail, e.getMessage());
        }
    }

    // 📩 Booking Email
    @Async
    public void sendBookingConfirmation(String toEmail, String projectName, String date, String slotTime, String status) {
        String content = """
        <h2>Project Demo Slot Booking</h2>
        <p>Your booking status has been updated.</p>
        <ul>
            <li><b>Project:</b> %s</li>
            <li><b>Date:</b> %s</li>
            <li><b>Time:</b> %s</li>
            <li><b>Status:</b> %s</li>
        </ul>
        <p>Thank you,<br>Project Scheduling System</p>
        """.formatted(projectName, date, slotTime, status.toUpperCase());

        sendEmail(toEmail, "Project Slot Booking Status", content);
    }

    // 📩 Registration Email
    @Async
    public void sendRegistrationConfirmation(String toEmail, String projectName, String leaderName, int memberCount) {
        String content = """
        <h2>Team Registration Successful</h2>
        <p>Your team has been registered successfully.</p>
        <ul>
            <li><b>Project:</b> %s</li>
            <li><b>Leader:</b> %s</li>
            <li><b>Members:</b> %d</li>
        </ul>
        <p>You can now book your demo slot.</p>
        <p>Thank you,<br>Project Scheduling System</p>
        """.formatted(projectName, leaderName, memberCount);

        sendEmail(toEmail, "Team Registration Confirmed", content);
    }
}