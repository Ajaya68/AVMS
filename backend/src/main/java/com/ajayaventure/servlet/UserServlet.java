package com.ajayaventure.servlet;

import com.ajayaventure.audit.AuditService;
import com.ajayaventure.dto.CreateUserRequest;
import com.ajayaventure.security.AuthenticatedUser;
import com.ajayaventure.security.SecurityContext;
import com.ajayaventure.service.UserService;
import com.ajayaventure.util.Responses;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User administration.
 *
 * <pre>
 * GET  /api/v1/users          — paged user list (requires USER.VIEW)
 * POST /api/v1/users          — create user (requires USER.MANAGE)
 * </pre>
 */
@WebServlet("/api/v1/users")
public class UserServlet extends ApiServlet {

    private final UserService service = new UserService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        api(req, resp, (req0, resp0) -> {
            SecurityContext.requirePermission(req, "USER.VIEW");
            AuthenticatedUser user = SecurityContext.requireUser(req);
            int page = pageOf(req);
            int pageSize = pageSizeOf(req);
            long total = service.count(user.getOrganizationId());
            Map<String, Object> body = Responses.page(
                    service.list(user.getOrganizationId(), page, pageSize), total, page, pageSize);
            ok(req, resp, body);
        });
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        api(req, resp, (req0, resp0) -> {
            SecurityContext.requirePermission(req, "USER.MANAGE");
            AuthenticatedUser user = SecurityContext.requireUser(req);
            CreateUserRequest body = readJson(req, CreateUserRequest.class);
            var created = service.create(user.getOrganizationId(), user.getUserId(), body);

            auditService.record(req, user.getOrganizationId(), null,
                    "USER_CREATED", "USER", String.valueOf(created.getUserId()),
                    "SUCCESS", null, usernameOf(created));

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("user_id", created.getUserId());
            out.put("username", created.getUsername());
            out.put("status", created.getStatus());
            created(req, resp, out);
        });
    }

    private static String usernameOf(com.ajayaventure.model.User user) {
        return "username=" + user.getUsername()
                + ", email=" + user.getEmail()
                + ", status=" + user.getStatus();
    }
}