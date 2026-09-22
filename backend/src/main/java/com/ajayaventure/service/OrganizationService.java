package com.ajayaventure.service;

import com.ajayaventure.config.Database;
import com.ajayaventure.dao.OrganizationDao;
import com.ajayaventure.exception.ApiException;
import com.ajayaventure.model.Organization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrganizationService {

    private final OrganizationDao dao = new OrganizationDao();

    public List<Organization> listActive() {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return dao.list(conn);
        } catch (SQLException e) {
            throw ApiException.internal("Organizations could not be loaded.");
        }
    }

    public Organization findById(long organizationId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return dao.findById(conn, organizationId)
                    .orElseThrow(() -> ApiException.notFound("ORGANIZATION", "Organization not found."));
        } catch (SQLException e) {
            throw ApiException.internal("Organization could not be loaded.");
        }
    }
}