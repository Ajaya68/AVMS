package com.ajayaventure.servlet;

import com.ajayaventure.exception.ApiException;
import com.ajayaventure.security.SecurityContext;
import com.ajayaventure.service.OrganizationService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * <pre>
 * GET /api/v1/organizations       — list active organizations
 * GET /api/v1/organizations/{id}  — one organization
 * </pre>
 */
@WebServlet("/api/v1/organizations/*")
public class OrganizationServlet extends ApiServlet {

    private final OrganizationService service = new OrganizationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        api(req, resp, (req0, resp0) -> {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                SecurityContext.requirePermission(req, "ORG.VIEW");
                ok(req, resp, service.listActive());
                return;
            }
            long id = parseId(path.substring(1));
            SecurityContext.requirePermission(req, "ORG.VIEW");
            ok(req, resp, service.findById(id));
        });
    }

    private static long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("ID", "Invalid organization id.");
        }
    }
}