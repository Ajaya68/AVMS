package com.ajayaventure.dao;

import com.ajayaventure.dto.BusinessUnitAccess;
import com.ajayaventure.model.Permission;
import com.ajayaventure.model.Role;
import com.ajayaventure.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final String COLUMNS =
            "USER_ID, ORGANIZATION_ID, EMPLOYEE_ID, USERNAME, EMAIL, MOBILE, PASSWORD_HASH, "
                    + "FIRST_NAME, LAST_NAME, STATUS, FAILED_LOGIN_COUNT, LOCKED_UNTIL, "
                    + "LAST_LOGIN_AT, PASSWORD_CHANGED_AT, CREATED_AT, UPDATED_AT, VERSION_NO";

    public Optional<User> findByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_USER WHERE USERNAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_USER WHERE EMAIL = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findById(Connection conn, long userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_USER WHERE USER_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO AV_USER (ORGANIZATION_ID, EMPLOYEE_ID, USERNAME, EMAIL, MOBILE, "
                + "PASSWORD_HASH, FIRST_NAME, LAST_NAME, STATUS, FAILED_LOGIN_COUNT) "
                + "VALUES (?,?,?,?,?,?,?,?,?,0)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"USER_ID"})) {
            ps.setLong(1, user.getOrganizationId());
            ps.setObject(2, user.getEmployeeId(), java.sql.Types.NUMERIC);
            ps.setString(3, user.getUsername().toLowerCase().trim());
            ps.setString(4, user.getEmail().toLowerCase().trim());
            ps.setString(5, user.getMobile());
            ps.setString(6, user.getPasswordHash());
            ps.setString(7, user.getFirstName());
            ps.setString(8, user.getLastName());
            ps.setString(9, user.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("No generated key for AV_USER");
        }
    }

    public void registerFailedLogin(Connection conn, long userId, int failedCount, Timestamp lockedUntil) throws SQLException {
        String sql = "UPDATE AV_USER SET FAILED_LOGIN_COUNT = ?, LOCKED_UNTIL = ?, UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE USER_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, failedCount);
            ps.setTimestamp(2, lockedUntil);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void registerLoginSuccess(Connection conn, long userId) throws SQLException {
        String sql = "UPDATE AV_USER SET FAILED_LOGIN_COUNT = 0, LOCKED_UNTIL = NULL, "
                + "LAST_LOGIN_AT = CURRENT_TIMESTAMP, UPDATED_AT = CURRENT_TIMESTAMP WHERE USER_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public void updatePassword(Connection conn, long userId, String newHash) throws SQLException {
        String sql = "UPDATE AV_USER SET PASSWORD_HASH = ?, PASSWORD_CHANGED_AT = CURRENT_TIMESTAMP, "
                + "UPDATED_AT = CURRENT_TIMESTAMP WHERE USER_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void insertUserRole(Connection conn, long userId, long roleId) throws SQLException {
        String sql = "INSERT INTO AV_USER_ROLE (USER_ID, ROLE_ID) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, roleId);
            ps.executeUpdate();
        }
    }

    public void insertUserBusinessUnit(Connection conn, long userId, long organizationId, long businessUnitId,
                                       String accessLevel, Long createdBy) throws SQLException {
        String sql = "INSERT INTO AV_USER_BUSINESS_UNIT (USER_ID, ORGANIZATION_ID, BUSINESS_UNIT_ID, "
                + "ACCESS_LEVEL, STATUS, CREATED_BY) VALUES (?,?,?,?, 'ACTIVE', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, organizationId);
            ps.setLong(3, businessUnitId);
            ps.setString(4, accessLevel);
            ps.setObject(5, createdBy, java.sql.Types.NUMERIC);
            ps.executeUpdate();
        }
    }

    public boolean hasAccessToBusinessUnit(Connection conn, long userId, long organizationId, long businessUnitId)
            throws SQLException {
        String sql = "SELECT 1 FROM AV_USER_BUSINESS_UNIT "
                + "WHERE USER_ID = ? AND ORGANIZATION_ID = ? AND BUSINESS_UNIT_ID = ? AND STATUS = 'ACTIVE' "
                + "AND ROWNUM = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, organizationId);
            ps.setLong(3, businessUnitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Role> findRoles(Connection conn, long userId) throws SQLException {
        String sql = "SELECT r.ROLE_ID, r.ORGANIZATION_ID, r.ROLE_CODE, r.ROLE_NAME, r.DESCRIPTION, r.STATUS "
                + "FROM AV_ROLE r JOIN AV_USER_ROLE ur ON ur.ROLE_ID = r.ROLE_ID "
                + "WHERE ur.USER_ID = ? ORDER BY r.ROLE_ID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Role> roles = new ArrayList<>();
                while (rs.next()) {
                    Role role = new Role();
                    role.setRoleId(rs.getLong("ROLE_ID"));
                    role.setOrganizationId(rs.getLong("ORGANIZATION_ID") == 0 ? null : rs.getLong("ORGANIZATION_ID"));
                    role.setRoleCode(rs.getString("ROLE_CODE"));
                    role.setRoleName(rs.getString("ROLE_NAME"));
                    role.setDescription(rs.getString("DESCRIPTION"));
                    role.setStatus(rs.getString("STATUS"));
                    roles.add(role);
                }
                return roles;
            }
        }
    }

    public List<Permission> findPermissions(Connection conn, long userId) throws SQLException {
        String sql = "SELECT DISTINCT p.PERMISSION_ID, p.PERMISSION_CODE, p.PERMISSION_NAME, "
                + "p.MODULE_CODE, p.ACTION_CODE "
                + "FROM AV_PERMISSION p "
                + "JOIN AV_ROLE_PERMISSION rp ON rp.PERMISSION_ID = p.PERMISSION_ID "
                + "JOIN AV_USER_ROLE ur ON ur.ROLE_ID = rp.ROLE_ID "
                + "WHERE ur.USER_ID = ? ORDER BY p.PERMISSION_CODE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Permission> perms = new ArrayList<>();
                while (rs.next()) {
                    Permission p = new Permission();
                    p.setPermissionId(rs.getLong("PERMISSION_ID"));
                    p.setPermissionCode(rs.getString("PERMISSION_CODE"));
                    p.setPermissionName(rs.getString("PERMISSION_NAME"));
                    p.setModuleCode(rs.getString("MODULE_CODE"));
                    p.setActionCode(rs.getString("ACTION_CODE"));
                    perms.add(p);
                }
                return perms;
            }
        }
    }

    /** Business-unit access projections for the logged-in user, with names. */
    public List<BusinessUnitAccess> listBusinessUnitAccess(Connection conn, long organizationId, long userId)
            throws SQLException {
        String sql = "SELECT ub.BUSINESS_UNIT_ID, bu.BUSINESS_UNIT_CODE, bu.BUSINESS_UNIT_NAME, "
                + "bu.BUSINESS_UNIT_TYPE, ub.ACCESS_LEVEL "
                + "FROM AV_USER_BUSINESS_UNIT ub "
                + "JOIN AV_BUSINESS_UNIT bu ON bu.BUSINESS_UNIT_ID = ub.BUSINESS_UNIT_ID "
                + "WHERE ub.ORGANIZATION_ID = ? AND ub.USER_ID = ? AND ub.STATUS = 'ACTIVE' AND bu.STATUS = 'ACTIVE' "
                + "ORDER BY ub.BUSINESS_UNIT_ID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BusinessUnitAccess> accesses = new ArrayList<>();
                while (rs.next()) {
                    accesses.add(new BusinessUnitAccess(
                            rs.getLong("BUSINESS_UNIT_ID"),
                            rs.getString("BUSINESS_UNIT_CODE"),
                            rs.getString("BUSINESS_UNIT_NAME"),
                            rs.getString("BUSINESS_UNIT_TYPE"),
                            rs.getString("ACCESS_LEVEL")));
                }
                return accesses;
            }
        }
    }

    public long count(Connection conn, long organizationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AV_USER WHERE ORGANIZATION_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    public List<User> list(Connection conn, long organizationId, int offset, int limit) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM AV_USER WHERE ORGANIZATION_ID = ? "
                + "ORDER BY USER_ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(map(rs));
                }
                return users;
            }
        }
    }

    static User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("USER_ID"));
        user.setOrganizationId(rs.getLong("ORGANIZATION_ID"));
        long empId = rs.getLong("EMPLOYEE_ID");
        user.setEmployeeId(rs.wasNull() ? null : empId);
        user.setUsername(rs.getString("USERNAME"));
        user.setEmail(rs.getString("EMAIL"));
        user.setMobile(rs.getString("MOBILE"));
        user.setPasswordHash(rs.getString("PASSWORD_HASH"));
        user.setFirstName(rs.getString("FIRST_NAME"));
        user.setLastName(rs.getString("LAST_NAME"));
        user.setStatus(rs.getString("STATUS"));
        user.setFailedLoginCount(rs.getInt("FAILED_LOGIN_COUNT"));
        user.setLockedUntil(rs.getTimestamp("LOCKED_UNTIL") == null ? null : rs.getTimestamp("LOCKED_UNTIL").toLocalDateTime());
        user.setLastLoginAt(rs.getTimestamp("LAST_LOGIN_AT") == null ? null : rs.getTimestamp("LAST_LOGIN_AT").toLocalDateTime());
        user.setPasswordChangedAt(rs.getTimestamp("PASSWORD_CHANGED_AT") == null ? null : rs.getTimestamp("PASSWORD_CHANGED_AT").toLocalDateTime());
        user.setCreatedAt(rs.getTimestamp("CREATED_AT") == null ? null : rs.getTimestamp("CREATED_AT").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("UPDATED_AT") == null ? null : rs.getTimestamp("UPDATED_AT").toLocalDateTime());
        user.setVersionNo(rs.getLong("VERSION_NO"));
        return user;
    }
}