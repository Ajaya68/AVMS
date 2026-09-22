package com.ajayaventure.audit;

import com.ajayaventure.config.Database;
import com.ajayaventure.util.Responses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Writes {@code AV_AUDIT_EVENT} rows. Best-effort within the same transaction
 * as the business mutation when a connection is passed; otherwise a separate
 * short transaction is used so a failed audit never blocks business flow.
 */
public class AuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("com.ajayaventure.audit");
    private final AuditDao auditDao = new AuditDao();

    public void record(HttpServletRequest request, String actionCode, String entityType,
                       String entityId, String outcome, String afterSummary) {
        record(request, null, null, actionCode, entityType, entityId, outcome, null, afterSummary);
    }

    public void record(HttpServletRequest request, Long organizationId, Long businessUnitId,
                       String actionCode, String entityType, String entityId, String outcome,
                       String beforeSummary, String afterSummary) {
        AuditEvent event = AuditEvent.builder()
                .organizationId(organizationId)
                .businessUnitId(businessUnitId)
                .userId(currentUserId(request))
                .requestId(Responses.requestId(request))
                .action(actionCode)
                .entity(entityType, entityId)
                .outcome(outcome)
                .ip(remoteIp(request))
                .agent(safeLen(request.getHeader("User-Agent"), 255))
                .before(beforeSummary)
                .after(afterSummary)
                .build();

        String line = String.format("%s|%s|%s|%s|org=%s bu=%s user=%s outcome=%s",
                event.getActionCode(), event.getEntityType(), event.getEntityId(),
                event.getRequestId(), event.getOrganizationId(), event.getBusinessUnitId(),
                event.getUserId(), event.getOutcome());
        auditLog.info(line);

        try (Connection conn = Database.getDataSource().getConnection()) {
            auditDao.insert(conn, event);
        } catch (SQLException e) {
            auditLog.warn("Failed to persist audit event ({} {}): {}", actionCode, entityId, e.getMessage());
        }
    }

    private static Long currentUserId(HttpServletRequest request) {
        Object principal = request.getAttribute("av.authenticated");
        if (principal instanceof com.ajayaventure.security.AuthenticatedUser user) {
            return user.getUserId();
        }
        return null;
    }

    private static String remoteIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String safeLen(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}