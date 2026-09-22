package com.ajayaventure.dao;

import com.ajayaventure.config.Database;
import com.ajayaventure.model.Organization;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrganizationDao {

    private static final String COLUMNS =
            "ORGANIZATION_ID, ORGANIZATION_CODE, ORGANIZATION_NAME, LEGAL_NAME, DESCRIPTION, "
                    + "TAX_IDENTIFIER, EMAIL, PHONE, WEBSITE, ADDRESS_LINE_1, ADDRESS_LINE_2, "
                    + "CITY, STATE_REGION, COUNTRY, POSTAL_CODE, DEFAULT_CURRENCY_CODE, "
                    + "DEFAULT_TIME_ZONE, STATUS, CREATED_AT, UPDATED_AT, VERSION_NO";

    public List<Organization> list(Connection conn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_ORGANIZATION WHERE STATUS = 'ACTIVE' ORDER BY ORGANIZATION_ID";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Organization> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    public Optional<Organization> findById(Connection conn, long organizationId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_ORGANIZATION WHERE ORGANIZATION_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Organization> findByCode(Connection conn, String code) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_ORGANIZATION WHERE ORGANIZATION_CODE = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Connection conn, Organization org) throws SQLException {
        String sql = "INSERT INTO AV_ORGANIZATION (ORGANIZATION_CODE, ORGANIZATION_NAME, LEGAL_NAME, "
                + "DESCRIPTION, TAX_IDENTIFIER, EMAIL, PHONE, WEBSITE, ADDRESS_LINE_1, ADDRESS_LINE_2, "
                + "CITY, STATE_REGION, COUNTRY, POSTAL_CODE, DEFAULT_CURRENCY_CODE, DEFAULT_TIME_ZONE, "
                + "STATUS, CREATED_BY, UPDATED_BY) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ORGANIZATION_ID"})) {
            ps.setString(1, org.getOrganizationCode());
            ps.setString(2, org.getOrganizationName());
            ps.setString(3, org.getLegalName());
            ps.setString(4, org.getDescription());
            ps.setString(5, org.getTaxIdentifier());
            ps.setString(6, org.getEmail());
            ps.setString(7, org.getPhone());
            ps.setString(8, org.getWebsite());
            ps.setString(9, org.getAddressLine1());
            ps.setString(10, org.getAddressLine2());
            ps.setString(11, org.getCity());
            ps.setString(12, org.getStateRegion());
            ps.setString(13, org.getCountry());
            ps.setString(14, org.getPostalCode());
            ps.setString(15, org.getDefaultCurrencyCode());
            ps.setString(16, org.getDefaultTimeZone());
            ps.setString(17, org.getStatus());
            ps.setObject(18, org.getCreatedBy() == 0 ? null : org.getCreatedBy(), java.sql.Types.NUMERIC);
            ps.setObject(19, org.getUpdatedBy() == 0 ? null : org.getUpdatedBy(), java.sql.Types.NUMERIC);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("No generated key for AV_ORGANIZATION");
        }
    }

    public int update(Connection conn, Organization org) throws SQLException {
        String sql = "UPDATE AV_ORGANIZATION SET ORGANIZATION_NAME = ?, LEGAL_NAME = ?, DESCRIPTION = ?, "
                + "TAX_IDENTIFIER = ?, EMAIL = ?, PHONE = ?, WEBSITE = ?, ADDRESS_LINE_1 = ?, ADDRESS_LINE_2 = ?, "
                + "CITY = ?, STATE_REGION = ?, COUNTRY = ?, POSTAL_CODE = ?, DEFAULT_CURRENCY_CODE = ?, "
                + "DEFAULT_TIME_ZONE = ?, STATUS = ?, UPDATED_AT = CURRENT_TIMESTAMP, UPDATED_BY = ?, "
                + "VERSION_NO = VERSION_NO + 1 "
                + "WHERE ORGANIZATION_ID = ? AND VERSION_NO = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, org.getOrganizationName());
            ps.setString(2, org.getLegalName());
            ps.setString(3, org.getDescription());
            ps.setString(4, org.getTaxIdentifier());
            ps.setString(5, org.getEmail());
            ps.setString(6, org.getPhone());
            ps.setString(7, org.getWebsite());
            ps.setString(8, org.getAddressLine1());
            ps.setString(9, org.getAddressLine2());
            ps.setString(10, org.getCity());
            ps.setString(11, org.getStateRegion());
            ps.setString(12, org.getCountry());
            ps.setString(13, org.getPostalCode());
            ps.setString(14, org.getDefaultCurrencyCode());
            ps.setString(15, org.getDefaultTimeZone());
            ps.setString(16, org.getStatus());
            ps.setObject(17, org.getUpdatedBy() == 0 ? null : org.getUpdatedBy(), java.sql.Types.NUMERIC);
            ps.setLong(18, org.getOrganizationId());
            ps.setLong(19, org.getVersionNo());
            return ps.executeUpdate();
        }
    }

    static Organization map(ResultSet rs) throws SQLException {
        Organization org = new Organization();
        org.setOrganizationId(rs.getLong("ORGANIZATION_ID"));
        org.setOrganizationCode(rs.getString("ORGANIZATION_CODE"));
        org.setOrganizationName(rs.getString("ORGANIZATION_NAME"));
        org.setLegalName(rs.getString("LEGAL_NAME"));
        org.setDescription(rs.getString("DESCRIPTION"));
        org.setTaxIdentifier(rs.getString("TAX_IDENTIFIER"));
        org.setEmail(rs.getString("EMAIL"));
        org.setPhone(rs.getString("PHONE"));
        org.setWebsite(rs.getString("WEBSITE"));
        org.setAddressLine1(rs.getString("ADDRESS_LINE_1"));
        org.setAddressLine2(rs.getString("ADDRESS_LINE_2"));
        org.setCity(rs.getString("CITY"));
        org.setStateRegion(rs.getString("STATE_REGION"));
        org.setCountry(rs.getString("COUNTRY"));
        org.setPostalCode(rs.getString("POSTAL_CODE"));
        org.setDefaultCurrencyCode(rs.getString("DEFAULT_CURRENCY_CODE"));
        org.setDefaultTimeZone(rs.getString("DEFAULT_TIME_ZONE"));
        org.setStatus(rs.getString("STATUS"));
        org.setCreatedAt(rs.getTimestamp("CREATED_AT") == null ? null : rs.getTimestamp("CREATED_AT").toLocalDateTime());
        org.setUpdatedAt(rs.getTimestamp("UPDATED_AT") == null ? null : rs.getTimestamp("UPDATED_AT").toLocalDateTime());
        org.setVersionNo(rs.getLong("VERSION_NO"));
        return org;
    }

    /** Convenience mapping for statements/rows defined outside this DAO. */
    static Organization mapRow(ResultSet rs) throws SQLException {
        return map(rs);
    }
}