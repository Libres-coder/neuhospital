package com.neusoft.hospital.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a request-scoped traceId and binds it to:
 *   - SLF4J MDC, so every log line carries it.
 *   - the response header X-Trace-Id, so callers can quote it when reporting issues.
 *
 * If the caller already supplies X-Trace-Id, we honor it (for upstream-traced traffic).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String traceId = req.getHeader(HEADER);
        if (traceId == null || traceId.isBlank() || traceId.length() > 64) {
            traceId = generate();
        }
        MDC.put(MDC_KEY, traceId);
        resp.setHeader(HEADER, traceId);
        try {
            chain.doFilter(req, resp);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String generate() {
        // 24 hex chars (UUID without dashes), short enough for logs, collision-safe
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}