package com.scheduling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration.
 * - Allows credentials (session cookies) to be sent cross-origin.
 * - setAllowedOriginPatterns("*") is used with credentials=true
 *   (setAllowedOrigins("*") cannot be used alongside credentials).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Must use patterns (not wildcard string) when allowCredentials=true
        config.setAllowedOriginPatterns(List.of("*"));

        // Allow session cookies / Authorization headers from browser
        config.setAllowCredentials(true);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));

        // Expose headers the browser may need to read
        config.setExposedHeaders(List.of(
                "Content-Disposition",
                "Content-Type",
                "Authorization"
        ));

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        System.out.println("[CORS] CorsFilter initialized: allowCredentials=true, allowedOriginPatterns=*");
        return new CorsFilter(source);
    }
}
