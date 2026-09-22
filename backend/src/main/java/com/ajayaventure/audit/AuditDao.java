package com.ajayaventure.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Persists {@code AV_AUDIT_EVENT}. Only INSERT is exposed; rows are never
 * editable by business users. Passwords, tokens and secrets must never be
 * included in the summary fields.
 */
public class AuditDao {

    public void insert(Connection conn, AuditEvent event) throws SQLException {
        String sql = "INSERT INTO AV_AUDIT_EVENT (ORGANIZATION_ID, BUSINESS_UNIT_ID, USER_ID, REQUEST_ID, "
                + "ACTION_CODE, ENTITY_TYPE, ENTITY_ID, OUTCOME, IP_ADDRESS, USER_AGENT, "
                + "BEFORE_SUMMARY, AFTER_SUMMARY) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, event.getOrganizationId(), java.sql.Types.NUMERIC);
            ps.setObject(2, event.getBusinessUnitId(), java.sql.Types.NUMERIC);
            ps.setObject(3, event.getUserId(), java.sql.Types.NUMERIC);
            ps.setString(4, event.getRequestId());
            ps.setString(5, event.getActionCode());
            ps.setString(6, event.getEntityType());
            ps.setString(7, event.getEntityId());
            ps.setString(8, event.getOutcome());
            ps.setString(9, event.getIpAddress());
            ps.setString(10, truncate(event.getUserAgent(), 255));
            ps.setString(11, truncate(event.getBeforeSummary(), 2000));
            ps.setString(12, truncate(event.getAfterSummary(), 2000));
            ps.executeUpdate();
        }
    }

    /** Dedicated connection, used when auditing outside an existing transaction. */
    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}