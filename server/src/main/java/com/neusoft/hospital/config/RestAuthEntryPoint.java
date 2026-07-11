package com.neusoft.hospital.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.hospital.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns our unified {@link Result} envelope (with traceId) when Spring Security
 * rejects a request with 401 / 403 — otherwise the client gets a Spring-default
 * plain {"timestamp","status","error","path"} body that breaks the contract.
 */
@Component
@RequiredArgsConstructor
public class RestAuthEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse resp, AuthenticationException ex)
            throws IOException {
        write(resp, HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, AccessDeniedException ex)
            throws IOException {
        write(resp, HttpStatus.FORBIDDEN, "无权访问该资源");
    }

    private void write(HttpServletResponse resp, HttpStatus status, String msg) throws IOException {
        resp.setStatus(status.value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getOutputStream(), Result.fail(status.value(), msg));
    }
}