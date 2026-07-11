package com.neusoft.hospital.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // preflight everywhere
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // public auth + browse
                        .requestMatchers(
                                "/api/auth/sms",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/departments/**", "/api/doctors/**").permitAll()
                        // triage is intentionally public so the demo path works
                        // without login (the SDK still learns what users describe).
                        .requestMatchers(HttpMethod.POST, "/api/preconsult/triage").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/preconsult/triage/image").permitAll()
                        // uploaded symptom photos must be reachable by Qwen-VL
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // actuator: only liveness/readiness are public, the rest stays 401
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        // openapi / swagger (dev only — restrict by network in prod)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthEntryPoint)
                        .accessDeniedHandler(restAuthEntryPoint)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}