package com.ajayaventure.service;

import com.ajayaventure.config.Database;
import com.ajayaventure.dao.RoleDao;
import com.ajayaventure.exception.ApiException;
import com.ajayaventure.model.Role;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class RoleService {

    private final RoleDao dao = new RoleDao();

    public List<Role> list(long organizationId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return dao.list(conn, organizationId);
        } catch (SQLException e) {
            throw ApiException.internal("Roles could not be loaded.");
        }
    }
}