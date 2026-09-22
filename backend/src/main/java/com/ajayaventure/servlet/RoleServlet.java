package com.ajayaventure.servlet;

import com.ajayaventure.security.SecurityContext;
import com.ajayaventure.service.RoleService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * <pre>GET /api/v1/roles — roles available within the user's organization.</pre>
 */
@WebServlet("/api/v1/roles")
public class RoleServlet extends ApiServlet {

    private final RoleService service = new RoleService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        api(req, resp, (req0, resp0) -> {
            SecurityContext.requirePermission(req, "USER.VIEW");
            SecurityContext.requirePermission(req, "BU.VIEW");
            long organizationId = SecurityContext.requireUser(req).getOrganizationId();
            ok(req, resp, service.list(organizationId));
        });
    }
}