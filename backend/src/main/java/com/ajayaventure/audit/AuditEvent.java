package com.ajayaventure.audit;

import java.time.LocalDateTime;

public class AuditEvent {

    private Long auditEventId;
    private Long organizationId;
    private Long businessUnitId;
    private Long userId;
    private String requestId;
    private LocalDateTime eventTime;
    private String actionCode;
    private String entityType;
    private String entityId;
    private String outcome;
    private String ipAddress;
    private String userAgent;
    private String beforeSummary;
    private String afterSummary;

    public static Builder builder() {
        return new Builder();
    }

    public Long getAuditEventId() {
        return auditEventId;
    }

    public void setAuditEventId(Long auditEventId) {
        this.auditEventId = auditEventId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getBusinessUnitId() {
        return businessUnitId;
    }

    public void setBusinessUnitId(Long businessUnitId) {
        this.businessUnitId = businessUnitId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getBeforeSummary() {
        return beforeSummary;
    }

    public void setBeforeSummary(String beforeSummary) {
        this.beforeSummary = beforeSummary;
    }

    public String getAfterSummary() {
        return afterSummary;
    }

    public void setAfterSummary(String afterSummary) {
        this.afterSummary = afterSummary;
    }

    public static final class Builder {
        private final AuditEvent event = new AuditEvent();

        public Builder organizationId(Long organizationId) {
            event.organizationId = organizationId;
            return this;
        }

        public Builder businessUnitId(Long businessUnitId) {
            event.businessUnitId = businessUnitId;
            return this;
        }

        public Builder userId(Long userId) {
            event.userId = userId;
            return this;
        }

        public Builder requestId(String requestId) {
            event.requestId = requestId;
            return this;
        }

        public Builder action(String actionCode) {
            event.actionCode = actionCode;
            return this;
        }

        public Builder entity(String entityType, String entityId) {
            event.entityType = entityType;
            event.entityId = entityId;
            return this;
        }

        public Builder outcome(String outcome) {
            event.outcome = outcome;
            return this;
        }

        public Builder ip(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public Builder agent(String userAgent) {
            event.userAgent = userAgent;
            return this;
        }

        public Builder before(String beforeSummary) {
            event.beforeSummary = beforeSummary;
            return this;
        }

        public Builder after(String afterSummary) {
            event.afterSummary = afterSummary;
            return this;
        }

        public AuditEvent build() {
            event.eventTime = LocalDateTime.now();
            return event;
        }
    }
}