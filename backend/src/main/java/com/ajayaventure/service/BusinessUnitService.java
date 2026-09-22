package com.ajayaventure.service;

import com.ajayaventure.config.Database;
import com.ajayaventure.dao.BusinessUnitDao;
import com.ajayaventure.exception.ApiException;
import com.ajayaventure.model.BusinessUnit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BusinessUnitService {

    private final BusinessUnitDao dao = new BusinessUnitDao();

    /**
     * Business units visible to the current user. A user only ever sees units
     * they are explicitly assigned to — this powers the UI selector and every
     * scoped API.
     */
    public List<BusinessUnit> listForUser(long organizationId, long userId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return dao.listAssignedToUser(conn, organizationId, userId);
        } catch (SQLException e) {
            throw ApiException.internal("Business units could not be loaded.");
        }
    }

    public List<BusinessUnit> listAll(long organizationId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return dao.listByOrganization(conn, organizationId);
        } catch (SQLException e) {
            throw ApiException.internal("Business units could not be loaded.");
        }
    }
}