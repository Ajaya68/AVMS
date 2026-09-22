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
        String env = (String) req.getServletContext().getAttribute("av.app.env");
        body.put("env", env);
        body.put("requestId", com.ajayaventure.util.Responses.requestId(req));

        int status = 200;
        if (ready) {
            DbProbeResult db = probeDatabase();
            body.put("ready", db.up);
            body.put("db", db.up ? "UP" : "DOWN");
            if ("development".equals(env)) {
                body.put("db_detail", db.detail);
            }
            if (!db.up) {
                status = 503;
            }
        }

        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(com.ajayaventure.util.Json.toApiJson(body));
    }

    private record DbProbeResult(boolean up, String detail) {
    }

    private DbProbeResult probeDatabase() {
        try {
            Connection connection = Database.getDataSource().getConnection();
            try (var st = connection.prepareStatement("SELECT 1 FROM DUAL");
                 var rs = st.executeQuery()) {
                boolean ok = rs.next();
                return new DbProbeResult(ok, ok ? "select ok" : "no row");
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            StringBuilder detail = new StringBuilder();
            detail.append(e.getClass().getSimpleName());
            Throwable t = e;
            int depth = 0;
            while (t != null && depth < 4) {
                if (t.getMessage() != null && !t.getMessage().isBlank()) {
                    detail.append(' ').append(t.getMessage());
                }
                t = t.getCause();
                depth++;
            }
            return new DbProbeResult(false, detail.toString());
        }
    }
}