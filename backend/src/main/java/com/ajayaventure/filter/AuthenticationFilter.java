package com.ajayaventure.filter;

import com.ajayaventure.security.AuthenticatedUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Session-based authentication for the API surface.
 *
 * <p>Public paths never require a session. Everything under {@code /api/v1}
 * does. The cached principal is bound to the request so downstream servlets
 * authorize against {@link com.ajayaventure.security.SecurityContext}.</p>
 */
public class AuthenticationFilter implements Filter {

    private static final Set<String> PUBLIC_API_PREFIXES = Set.of(
            "/api/v1/health",
            "/api/v1/auth/login"
    );

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rq;
        HttpServletResponse response = (HttpServletResponse) rs;
        String path = request.getRequestURI();

        if (!isProtected(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        AuthenticatedUser principal = session == null ? null
                : (AuthenticatedUser) session.getAttribute("principal");

        if (principal == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\","
                    + "\"message\":\"Authentication required.\"}}");
            return;
        }
        request.setAttribute("av.authenticated", principal);
        chain.doFilter(request, response);
    }

    private static boolean isProtected(String path) {
        if (!path.startsWith("/api/v1")) {
            return false;
        }
        for (String prefix : PUBLIC_API_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }
}