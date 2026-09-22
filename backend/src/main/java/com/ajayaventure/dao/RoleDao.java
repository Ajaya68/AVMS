package com.ajayaventure.dao;

import com.ajayaventure.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoleDao {

    public List<Role> list(Connection conn, long organizationId) throws SQLException {
        String sql = "SELECT ROLE_ID, ORGANIZATION_ID, ROLE_CODE, ROLE_NAME, DESCRIPTION, STATUS "
                + "FROM AV_ROLE WHERE STATUS = 'ACTIVE' AND (ORGANIZATION_ID IS NULL OR ORGANIZATION_ID = ?) "
                + "ORDER BY ROLE_ID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, organizationId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Role> roles = new ArrayList<>();
                while (rs.next()) {
                    roles.add(map(rs));
                }
                return roles;
            }
        }
    }

    static Role map(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setRoleId(rs.getLong("ROLE_ID"));
        role.setOrganizationId(rs.getLong("ORGANIZATION_ID") == 0 ? null : rs.getLong("ORGANIZATION_ID"));
        role.setRoleCode(rs.getString("ROLE_CODE"));
        role.setRoleName(rs.getString("ROLE_NAME"));
        role.setDescription(rs.getString("DESCRIPTION"));
        role.setStatus(rs.getString("STATUS"));
        return role;
    }
}