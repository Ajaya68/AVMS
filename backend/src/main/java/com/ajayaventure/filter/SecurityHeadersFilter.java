package com.ajayaventure.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Applies hardened HTTP security response headers on every response.
 * Some are set below only when not already present so a reverse proxy (Nginx)
 * may supply stricter values.
 */
public class SecurityHeadersFilter implements Filter {

    private static final String[][] HEADERS = {
            {"X-Content-Type-Options", "nosniff"},
            {"X-Frame-Options", "DENY"},
            {"Referrer-Policy", "strict-origin-when-cross-origin"},
            {"Permissions-Policy", "camera=(), microphone=(), geolocation=()"},
            {"Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; base-uri 'self'"},
            {"Cross-Origin-Opener-Policy", "same-origin"},
    };

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) rs;
        for (String[] pair : HEADERS) {
            if (response.getHeader(pair[0]) == null) {
                response.setHeader(pair[0], pair[1]);
            }
        }
        chain.doFilter(rq, response);
    }
}