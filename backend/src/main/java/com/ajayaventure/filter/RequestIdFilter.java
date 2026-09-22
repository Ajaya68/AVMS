package com.ajayaventure.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation request-id to every request and emits it as the
 * {@code X-Request-Id} response header and structured log marker.
 */
public class RequestIdFilter implements Filter {

    public static final String HEADER = "X-Request-Id";

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rq;
        HttpServletResponse response = (HttpServletResponse) rs;

        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader(HEADER, requestId);

        org.slf4j.MDC.put("requestId", requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove("requestId");
        }
    }
}