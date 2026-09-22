package com.ajayaventure.servlet;

import com.ajayaventure.audit.AuditService;
import com.ajayaventure.dto.AuthResponse;
import com.ajayaventure.dto.ChangePasswordRequest;
import com.ajayaventure.dto.LoginRequest;
import com.ajayaventure.exception.ApiException;
import com.ajayaventure.security.AuthenticatedUser;
import com.ajayaventure.service.AuthService;
import com.ajayaventure.service.UserService;
import com.ajayaventure.util.EnvVars;
import com.ajayaventure.util.Responses;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Authentication endpoints.
 *
 * <pre>
 * POST /api/v1/auth/login            (public)
 * POST /api/v1/auth/logout
 * GET  /api/v1/auth/me
 * POST /api/v1/auth/change-password
 * </pre>
 *
 * <p>Sessions are rotated on login (session-fixation defense). The principal is
 * cached server-side in the session; only safe projections are serialized.</p>
 */
@WebServlet(urlPatterns = {
        "/api/v1/auth/login",
        "/api/v1/auth/logout",
        "/api/v1/auth/me",
        "/api/v1/auth/change-password"
})
public class AuthServlet extends ApiServlet {

    private static final String SESSION_PRINCIPAL = "principal";

    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getRequestURI();
        if (path.endsWith("/login")) {
            api(req, resp, (req0, resp0) -> login(req, resp));
        } else if (path.endsWith("/logout")) {
            api(req, resp, (req0, resp0) -> logout(req, resp));
        } else if (path.endsWith("/change-password")) {
            api(req, resp, (req0, resp0) -> changePassword(req, resp));
        } else {
            Responses.sendError(resp, req, ApiException.notFound("ROUTE", "Unknown route."));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getRequestURI().endsWith("/me")) {
            api(req, resp, (req0, resp0) -> me(req, resp));
        } else {
            Responses.sendError(resp, req, ApiException.notFound("ROUTE", "Unknown route."));
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        LoginRequest body = readJson(req, LoginRequest.class);
        AuthenticatedUser principal = authService.login(body.getUsername(), body.getPassword());

        // Session rotation: prevent session fixation / invalidate any pre-auth session.
        req.getSession(true);
        req.changeSessionId();
        HttpSession session = req.getSession(false);
        session.setAttribute(SESSION_PRINCIPAL, principal);
        session.setMaxInactiveInterval(EnvVars.getInt("SESSION_TIMEOUT_MINUTES", 30) * 60);

        auditService.record(req, principal.getOrganizationId(), null,
                "USER_LOGIN", "USER", String.valueOf(principal.getUserId()),
                "SUCCESS", null, null);

        AuthResponse response = AuthResponse.from(principal,
                userService.businessUnitAccess(principal.getOrganizationId(), principal.getUserId()));
        ok(req, resp, response);
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        AuthenticatedUser principal = com.ajayaventure.security.SecurityContext.currentUser(req);
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        if (principal != null) {
            auditService.record(req, principal.getOrganizationId(), null,
                    "USER_LOGOUT", "USER", String.valueOf(principal.getUserId()), "SUCCESS", null, null);
        }
        ok(req, resp, java.util.Map.of("logged_out", true));
    }

    private void me(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        AuthenticatedUser principal = com.ajayaventure.security.SecurityContext.currentUser(req);
        if (principal == null) {
            throw ApiException.unauthorized("Authentication required.");
        }
        AuthResponse response = AuthResponse.from(principal,
                userService.businessUnitAccess(principal.getOrganizationId(), principal.getUserId()));
        ok(req, resp, response);
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        AuthenticatedUser principal = com.ajayaventure.security.SecurityContext.requireUser(req);
        ChangePasswordRequest body = readJson(req, ChangePasswordRequest.class);
        authService.changePassword(principal.getUserId(), body.getCurrentPassword(), body.getNewPassword());

        // Reload principal so mustChangePassword flips to false in this session.
        AuthenticatedUser refreshed = authService.refresh(principal.getUserId());
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.setAttribute(SESSION_PRINCIPAL, refreshed);
        }

        auditService.record(req, principal.getOrganizationId(), null,
                "PASSWORD_CHANGED", "USER", String.valueOf(principal.getUserId()), "SUCCESS", null, null);
        ok(req, resp, java.util.Map.of("changed", true));
    }
}