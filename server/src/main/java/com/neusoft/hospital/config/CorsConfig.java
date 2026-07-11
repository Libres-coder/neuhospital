package com.neusoft.hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS is driven by `app.cors.allowed-origins` (comma-separated).
 *  - dev profile:    set to "*" (default in application.yml)
 *  - prod profile:   set to "https://hosp.example.com,https://admin.example.com"
 *
 *  When credentials are required we fall back to per-origin echo (NOT "*"),
 *  which keeps cookies / Authorization headers usable.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                boolean wildcard = "*".equals(allowedOrigins.trim());
                List<String> origins = wildcard
                        ? List.of("*")
                        : Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();

                var registration = registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);

                if (wildcard) {
                    // allowedOriginPatterns lets us keep allowCredentials(false) safe
                    registration.allowedOriginPatterns("*").allowCredentials(allowCredentials);
                } else {
                    registration.allowedOrigins(origins.toArray(String[]::new))
                            .allowCredentials(allowCredentials);
                }
            }
        };
    }
}