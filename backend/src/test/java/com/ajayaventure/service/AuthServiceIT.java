package com.ajayaventure.service;

import com.ajayaventure.config.Database;
import com.ajayaventure.dao.BusinessUnitDao;
import com.ajayaventure.dao.OrganizationDao;
import com.ajayaventure.dao.RoleDao;
import com.ajayaventure.dao.UserDao;
import com.ajayaventure.dto.BusinessUnitAccess;
import com.ajayaventure.exception.ApiException;
import com.ajayaventure.exception.FieldError;
import com.ajayaventure.model.Organization;
import com.ajayaventure.security.AuthenticatedUser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests exercising DAO + service layers against the real Oracle
 * database (seeded migration data). Skips cleanly when Oracle is unreachable.
 */
class AuthServiceIT {

    @BeforeAll
    static void requireDatabase() {
        try (Connection c = Database.getDataSource().getConnection()) {
            Assumptions.assumeTrue(c.isValid(3), "Oracle database not reachable");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Oracle database not reachable: " + e.getMessage());
        }
    }

    @Test
    void seededAdminCanLogin() {
        AuthService auth = new AuthService();
        AuthenticatedUser principal = auth.login("admin", "ChangeMe!2026");
        assertNotNull(principal);
        assertEquals(1, principal.getUserId());
        assertTrue(principal.getPermissions().contains("ORG.VIEW"));
        assertTrue(principal.getPermissions().contains("USER.MANAGE"));
        assertEquals(2, principal.getBusinessUnits().size());
        assertTrue(principal.isMustChangePassword(), "seed admin has never changed password");
    }

    @Test
    void wrongPasswordRejected() {
        AuthService auth = new AuthService();
        ApiException error = assertThrows(ApiException.class,
                () -> auth.login("admin", "wrong-password"));
        assertEquals(401, error.getStatus());
    }

    @Test
    void seededMastersArePresent() throws Exception {
        try (Connection conn = Database.getDataSource().getConnection()) {
            OrganizationDao orgDao = new OrganizationDao();
            List<Organization> orgs = orgDao.list(conn);
            assertFalse(orgs.isEmpty());
            assertEquals("AJV", orgs.get(0).getOrganizationCode());

            BusinessUnitDao buDao = new BusinessUnitDao();
            assertEquals(2, buDao.listByOrganization(conn, 1).size());

            assertEquals(6, new RoleDao().list(conn, 1).size());
            assertFalse(new UserDao().findPermissions(conn, 1).isEmpty());
        }
    }

    @Test
    void adminAccessToBothBusinessUnits() throws Exception {
        try (Connection conn = Database.getDataSource().getConnection()) {
            UserDao dao = new UserDao();
            List<BusinessUnitAccess> access = dao.listBusinessUnitAccess(conn, 1, 1);
            assertEquals(2, access.size());
            assertEquals("ABMF", access.get(0).getBusinessUnitCode());
            assertEquals("AFF", access.get(1).getBusinessUnitCode());
            assertTrue(dao.hasAccessToBusinessUnit(conn, 1, 1, 1));
            assertTrue(dao.hasAccessToBusinessUnit(conn, 1, 1, 2));
        }
    }
}