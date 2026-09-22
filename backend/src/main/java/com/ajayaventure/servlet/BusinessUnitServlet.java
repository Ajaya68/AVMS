package com.ajayaventure.servlet;

import com.ajayaventure.exception.ApiException;
import com.ajayaventure.security.AuthenticatedUser;
import com.ajayaventure.security.SecurityContext;
import com.ajayaventure.service.BusinessUnitService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Business units visible to the current user.
 *
 * <pre>GET /api/v1/business-units — the user's assigned units (powers the selector)</pre>
 */
@WebServlet("/api/v1/business-units")
public class BusinessUnitServlet extends ApiServlet {

    private final BusinessUnitService service = new BusinessUnitService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        api(req, resp, (req0, resp0) -> {
            SecurityContext.requirePermission(req, "BU.VIEW");
            AuthenticatedUser user = SecurityContext.requireUser(req);
            ok(req, resp, service.listForUser(user.getOrganizationId(), user.getUserId()));
        });
    }
}