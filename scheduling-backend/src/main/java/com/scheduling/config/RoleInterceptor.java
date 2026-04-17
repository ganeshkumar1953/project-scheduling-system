package com.scheduling.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Validates session / X-Role header on protected API endpoints.
 * Admin APIs require a valid HTTP session with ROLE=ADMIN.
 * Booking write APIs require X-Role: STUDENT header.
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin@2026Secure}")
    private String adminPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow OPTIONS (CORS preflight) — never block pre-flight
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // ── Public endpoints ────────────────────────────────────────────
        if (path.startsWith("/api/auth/")) return true;
        if (path.startsWith("/api/students/register") && "POST".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/bookings/available") && "GET".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/bookings/all") && "GET".equalsIgnoreCase(method)) return true;

        String role = request.getHeader("X-Role");

        // ── Admin endpoints — must have a valid server-side session ─────
        if (path.startsWith("/api/admin/")) {
            jakarta.servlet.http.HttpSession session = request.getSession(false);

            // ── DEBUG LOGGING (leave in — useful for Render logs) ────────
            System.out.println("=== ADMIN AUTH CHECK ===");
            System.out.println("  Method+Path : " + method + " " + path);
            System.out.println("  Session ID  : " + (session != null ? session.getId() : "null — no session cookie received"));
            System.out.println("  Session ROLE: " + (session != null ? session.getAttribute("ROLE") : "N/A"));
            System.out.println("  Cookie hdr  : " + request.getHeader("Cookie"));
            System.out.println("  Origin hdr  : " + request.getHeader("Origin"));
            System.out.println("  X-Role hdr  : " + role);
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            System.out.println("  All headers :");
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    System.out.println("    " + name + ": " + request.getHeader(name));
                }
            }
            System.out.println("========================");
            // ────────────────────────────────────────────────────────────

            if (session == null || !"ADMIN".equals(session.getAttribute("ROLE"))) {
                System.out.println("[BLOCKED] No valid admin session for: " + path
                        + " | session=" + (session != null ? session.getId() : "null")
                        + " | ROLE=" + (session != null ? session.getAttribute("ROLE") : "N/A"));
                sendForbidden(response, "Access denied. Admin session required.");
                return false;
            }

            System.out.println("[ALLOWED] Admin session valid → " + path);
            return true;
        }

        // ── Student team lookup — allow always (used during login flow) ─
        if (path.matches("/api/students/.+/team") && "GET".equalsIgnoreCase(method)) {
            return true;
        }

        // ── Student bookings lookup ─────────────────────────────────────
        if (path.matches("/api/students/.+/bookings") && "GET".equalsIgnoreCase(method)) {
            if (!"STUDENT".equalsIgnoreCase(role)) {
                sendForbidden(response, "Access denied. Student login required.");
                return false;
            }
            return true;
        }

        // ── Booking write operations (POST/PUT/DELETE on /api/bookings/) ─
        if (path.startsWith("/api/bookings") && !("GET".equalsIgnoreCase(method))) {
            if (!"STUDENT".equalsIgnoreCase(role)) {
                sendForbidden(response, "Access denied. Student login required.");
                return false;
            }
            return true;
        }

        // Allow everything else
        return true;
    }

    private void sendForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", message)));
    }
}
