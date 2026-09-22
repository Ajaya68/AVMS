package com.ajayaventure.model;

import java.time.LocalDateTime;

public class UserBusinessUnit {

    private long userBusinessUnitId;
    private long userId;
    private long organizationId;
    private long businessUnitId;
    private String accessLevel;
    private String status;
    private LocalDateTime createdAt;
    private Long createdBy;

    public long getUserBusinessUnitId() {
        return userBusinessUnitId;
    }

    public void setUserBusinessUnitId(long userBusinessUnitId) {
        this.userBusinessUnitId = userBusinessUnitId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(long organizationId) {
        this.organizationId = organizationId;
    }

    public long getBusinessUnitId() {
        return businessUnitId;
    }

    public void setBusinessUnitId(long businessUnitId) {
        this.businessUnitId = businessUnitId;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}