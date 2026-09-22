package com.ajayaventure.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Set;
import java.util.Base64;

/**
 * Synchronizer-token CSRF protection.
 *
 * <p>Every authenticated session carries a random CSRF token. State-changing
 * requests (anything but GET/HEAD/OPTIONS) must echo it back in the
 * {@code X-XSRF-Token} header. Read requests are never blocked, so the token is
 * also exposed in the non-HttpOnly {@code AV-XSRF} cookie for the SPA to read.</p>
 */
public class CsrfFilter implements Filter {

    public static final String SESSION_ATTR = "csrfToken";
    public static final String HEADER_NAME = "X-XSRF-Token";
    public static final String COOKIE_NAME = "AV-XSRF";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rq;
        HttpServletResponse response = (HttpServletResponse) rs;
        String path = request.getRequestURI();

        // Auth bootstrap endpoints have their own protections; exempt them from
        // CSRF so first logins work without a pre-existing token.
        if (path.contains("/auth/login") || path.contains("/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }

        if (SAFE_METHODS.contains(request.getMethod())) {
            ensureToken(request, response);
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SESSION_ATTR) == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String expected = session.getAttribute(SESSION_ATTR).toString();
        String provided = request.getHeader(HEADER_NAME);
        if (provided == null || !MessageDigestSafe.equals(expected, provided)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void ensureToken(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        if (session.getAttribute(SESSION_ATTR) == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            session.setAttribute(SESSION_ATTR, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }
        String token = session.getAttribute(SESSION_ATTR).toString();
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setSecure(Boolean.parseBoolean(com.ajayaventure.util.EnvVars.get("COOKIE_SECURE", "false")));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /** Constant-time comparison to avoid timing side channels. */
    private static final class MessageDigestSafe {
        static boolean equals(String a, String b) {
            return java.security.MessageDigest.isEqual(
                    a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}