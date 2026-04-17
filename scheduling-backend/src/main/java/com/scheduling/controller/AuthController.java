package com.scheduling.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${ADMIN_USERNAME}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> adminLogin(
            @RequestBody Map<String, String> credentials,
            jakarta.servlet.http.HttpServletRequest request) {

        String username = credentials.getOrDefault("username", "");
        String password = credentials.getOrDefault("password", "");

        // ── DEBUG LOGGING ────────────────────────────────────────────────
        System.out.println("=== ADMIN LOGIN ATTEMPT ===");
        System.out.println("  Received username : " + username);
        System.out.println("  Expected username : " + adminUsername);
        System.out.println("  Password match    : " + adminPassword.equals(password));
        System.out.println("  Origin            : " + request.getHeader("Origin"));
        System.out.println("  Existing session  : " + (request.getSession(false) != null ? request.getSession(false).getId() : "none"));
        System.out.println("===========================");
        // ────────────────────────────────────────────────────────────────

        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            // Invalidate any old session and create a fresh one
            jakarta.servlet.http.HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            jakarta.servlet.http.HttpSession session = request.getSession(true);
            session.setAttribute("ROLE", "ADMIN");
            System.out.println("[LOGIN OK] Admin session created: " + session.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Login successful"));
        } else {
            System.out.println("[LOGIN FAIL] Invalid credentials for username: " + username);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid credentials"));
        }
    }
}
