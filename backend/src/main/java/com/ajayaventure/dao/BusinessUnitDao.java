package com.ajayaventure.dao;

import com.ajayaventure.model.BusinessUnit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BusinessUnitDao {

    private static final String COLUMNS =
            "BUSINESS_UNIT_ID, ORGANIZATION_ID, BUSINESS_UNIT_CODE, BUSINESS_UNIT_NAME, "
                    + "BUSINESS_UNIT_TYPE, DESCRIPTION, STATUS, START_DATE, END_DATE, "
                    + "CREATED_AT, UPDATED_AT, VERSION_NO";

    /** All active business units for an organization. */
    public List<BusinessUnit> listByOrganization(Connection conn, long organizationId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_BUSINESS_UNIT "
                + "WHERE ORGANIZATION_ID = ? AND STATUS = 'ACTIVE' ORDER BY BUSINESS_UNIT_ID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BusinessUnit> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    /** Business units assigned to a user (either via access records). */
    public List<BusinessUnit> listAssignedToUser(Connection conn, long organizationId, long userId) throws SQLException {
        String sql = "SELECT bu." + COLUMNS.replace(", ", ", bu.") + " FROM AV_BUSINESS_UNIT bu "
                + "JOIN AV_USER_BUSINESS_UNIT ub ON ub.BUSINESS_UNIT_ID = bu.BUSINESS_UNIT_ID "
                + "WHERE bu.ORGANIZATION_ID = ? AND ub.USER_ID = ? AND ub.STATUS = 'ACTIVE' AND bu.STATUS = 'ACTIVE' "
                + "ORDER BY bu.BUSINESS_UNIT_ID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BusinessUnit> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    public Optional<BusinessUnit> findById(Connection conn, long organizationId, long businessUnitId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_BUSINESS_UNIT WHERE ORGANIZATION_ID = ? AND BUSINESS_UNIT_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            ps.setLong(2, businessUnitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<BusinessUnit> findByCode(Connection conn, long organizationId, String code) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_BUSINESS_UNIT WHERE ORGANIZATION_ID = ? AND BUSINESS_UNIT_CODE = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Connection conn, BusinessUnit bu) throws SQLException {
        String sql = "INSERT INTO AV_BUSINESS_UNIT (ORGANIZATION_ID, BUSINESS_UNIT_CODE, BUSINESS_UNIT_NAME, "
                + "BUSINESS_UNIT_TYPE, DESCRIPTION, STATUS, START_DATE, END_DATE, CREATED_BY, UPDATED_BY) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"BUSINESS_UNIT_ID"})) {
            ps.setLong(1, bu.getOrganizationId());
            ps.setString(2, bu.getBusinessUnitCode());
            ps.setString(3, bu.getBusinessUnitName());
            ps.setString(4, bu.getBusinessUnitType());
            ps.setString(5, bu.getDescription());
            ps.setString(6, bu.getStatus());
            ps.setDate(7, bu.getStartDate() == null ? null : Date.valueOf(bu.getStartDate()));
            ps.setDate(8, bu.getEndDate() == null ? null : Date.valueOf(bu.getEndDate()));
            ps.setObject(9, bu.getCreatedBy() == 0 ? null : bu.getCreatedBy(), java.sql.Types.NUMERIC);
            ps.setObject(10, bu.getUpdatedBy() == 0 ? null : bu.getUpdatedBy(), java.sql.Types.NUMERIC);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("No generated key for AV_BUSINESS_UNIT");
        }
    }

    static BusinessUnit map(ResultSet rs) throws SQLException {
        BusinessUnit bu = new BusinessUnit();
        bu.setBusinessUnitId(rs.getLong("BUSINESS_UNIT_ID"));
        bu.setOrganizationId(rs.getLong("ORGANIZATION_ID"));
        bu.setBusinessUnitCode(rs.getString("BUSINESS_UNIT_CODE"));
        bu.setBusinessUnitName(rs.getString("BUSINESS_UNIT_NAME"));
        bu.setBusinessUnitType(rs.getString("BUSINESS_UNIT_TYPE"));
        bu.setDescription(rs.getString("DESCRIPTION"));
        bu.setStatus(rs.getString("STATUS"));
        bu.setStartDate(rs.getDate("START_DATE") == null ? null : rs.getDate("START_DATE").toLocalDate());
        bu.setEndDate(rs.getDate("END_DATE") == null ? null : rs.getDate("END_DATE").toLocalDate());
        bu.setCreatedAt(rs.getTimestamp("CREATED_AT") == null ? null : rs.getTimestamp("CREATED_AT").toLocalDateTime());
        bu.setUpdatedAt(rs.getTimestamp("UPDATED_AT") == null ? null : rs.getTimestamp("UPDATED_AT").toLocalDateTime());
        bu.setVersionNo(rs.getLong("VERSION_NO"));
        return bu;
    }
}