package com.scheduling.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Validates X-Role header on protected API endpoints.
 * Admin APIs require X-Role: ADMIN
 * Booking write APIs require X-Role: STUDENT
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

        // Allow OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // Allow public endpoints without role checks
        if (path.startsWith("/api/auth/")) return true;
        if (path.startsWith("/api/students/register") && "POST".equalsIgnoreCase(method)) return true;
        // Allow GET on /api/bookings/available and /api/bookings/all (public schedule)
        if (path.startsWith("/api/bookings/available") && "GET".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/bookings/all") && "GET".equalsIgnoreCase(method)) return true;

        String role = request.getHeader("X-Role");

        // Admin endpoints 
        if (path.startsWith("/api/admin/")) {
            if (!"ADMIN".equalsIgnoreCase(role)) {
                sendForbidden(response, "Access denied. Admin login required.");
                return false;
            }
            return true;
        }

        // Student team lookup and bookings lookup (require STUDENT role)  
        if (path.matches("/api/students/.+/team") && "GET".equalsIgnoreCase(method)) {
            // Allow this during student login (no role yet) or when role is STUDENT
            return true;
        }
        if (path.matches("/api/students/.+/bookings") && "GET".equalsIgnoreCase(method)) {
            if (!"STUDENT".equalsIgnoreCase(role)) {
                sendForbidden(response, "Access denied. Student login required.");
                return false;
            }
            return true;
        }

        // Booking write operations (POST, PUT, DELETE on /api/bookings/**)
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
