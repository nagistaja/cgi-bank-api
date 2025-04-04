package com.cgi.bank.account.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that adds request tracing information to the MDC context.
 * This enables correlation of log messages belonging to the same request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_PATH = "path";
    private static final String REQUEST_METHOD = "method";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = generateTraceId();
            
            MDC.put(TRACE_ID, traceId);
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());
            MDC.put(REQUEST_PATH, request.getRequestURI());
            MDC.put(REQUEST_METHOD, request.getMethod());
            
            response.addHeader("X-Trace-Id", traceId);
            
            log.debug("Starting request processing - {}", request.getRequestURI());
            
            filterChain.doFilter(request, response);
            
            log.debug("Completed request processing - {}", request.getRequestURI());
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Generates a trace ID for the request.
     * In a distributed system, this could check for an incoming trace ID from headers first.
     * 
     * @return A unique trace identifier
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
} 