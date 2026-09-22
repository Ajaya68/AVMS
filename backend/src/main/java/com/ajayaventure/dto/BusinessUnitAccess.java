package com.ajayaventure.dto;

/** Safe projection of a user's business-unit access for API responses. */
public class BusinessUnitAccess {

    private final long businessUnitId;
    private final String businessUnitCode;
    private final String businessUnitName;
    private final String businessUnitType;
    private final String accessLevel;

    public BusinessUnitAccess(long businessUnitId, String businessUnitCode, String businessUnitName,
                              String businessUnitType, String accessLevel) {
        this.businessUnitId = businessUnitId;
        this.businessUnitCode = businessUnitCode;
        this.businessUnitName = businessUnitName;
        this.businessUnitType = businessUnitType;
        this.accessLevel = accessLevel;
    }

    public long getBusinessUnitId() {
        return businessUnitId;
    }

    public String getBusinessUnitCode() {
        return businessUnitCode;
    }

    public String getBusinessUnitName() {
        return businessUnitName;
    }

    public String getBusinessUnitType() {
        return businessUnitType;
    }

    public String getAccessLevel() {
        return accessLevel;
    }
}