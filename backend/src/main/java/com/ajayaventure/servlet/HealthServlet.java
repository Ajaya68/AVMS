package com.ajayaventure.servlet;

import com.ajayaventure.config.AppContextListener;
import com.ajayaventure.config.Database;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liveness and readiness probes.
 *
 * <ul>
 *   <li>{@code GET /api/v1/health} — process liveness, always 200 while the app runs.</li>
 *   <li>{@code GET /api/v1/health/ready} — readiness; checks DB connectivity.</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"/api/v1/health", "/api/v1/health/ready"})
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean ready = req.getRequestURI().endsWith("/ready");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("app", AppContextListener.APP_NAME);
        body.put("version", AppContextListener.APP_VERSION);
        body.put("env", req.getServletContext().getAttribute("av.app.env"));
        body.put("requestId", com.ajayaventure.util.Responses.requestId(req));

        int status = 200;
        if (ready) {
            boolean dbUp = isDatabaseUp();
            body.put("ready", dbUp);
            body.put("db", dbUp ? "UP" : "DOWN");
            if (!dbUp) {
                status = 503;
            }
        }

        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(com.ajayaventure.util.Json.toApiJson(body));
    }

    private boolean isDatabaseUp() {
        try {
            Connection connection = Database.getDataSource().getConnection();
            try (var st = connection.prepareStatement("SELECT 1 FROM DUAL");
                 var rs = st.executeQuery()) {
                return rs.next();
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            return false;
        }
    }
}