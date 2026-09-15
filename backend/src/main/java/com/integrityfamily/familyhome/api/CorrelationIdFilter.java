package com.integrityfamily.familyhome.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Integer.MIN_VALUE) // Ensure this is executed before security filters
public final class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String correlationIdHeader = request.getHeader(CORRELATION_HEADER);
        String correlationId;

        if (correlationIdHeader != null) {
            correlationIdHeader = correlationIdHeader.trim();
            if (correlationIdHeader.length() == 36) {
                try {
                    correlationId = UUID.fromString(correlationIdHeader).toString();
                } catch (IllegalArgumentException ex) {
                    correlationId = UUID.randomUUID().toString();
                }
            } else {
                correlationId = UUID.randomUUID().toString();
            }
        } else {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
