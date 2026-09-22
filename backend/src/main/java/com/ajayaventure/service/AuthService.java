package com.ajayaventure.service;

import com.ajayaventure.config.Database;
import com.ajayaventure.dao.BusinessUnitDao;
import com.ajayaventure.dao.UserDao;
import com.ajayaventure.exception.ApiException;
import com.ajayaventure.model.BusinessUnit;
import com.ajayaventure.model.Permission;
import com.ajayaventure.model.Role;
import com.ajayaventure.model.User;
import com.ajayaventure.security.AuthenticatedUser;
import com.ajayaventure.security.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Authentication business logic. Enforces account lockout, generic error
 * messages (no username enumeration), and always works on server-side data.
 */
public class AuthService {

    public static final int MAX_FAILED_LOGINS = 5;
    public static final int LOCK_MINUTES = 15;
    public static final String GENERIC_LOGIN_ERROR = "Invalid username or password.";

    private final UserDao userDao = new UserDao();
    private final BusinessUnitDao businessUnitDao = new BusinessUnitDao();

    /**
     * Validates credentials and returns a fresh principal (session wiring is
     * handled by the caller). Throws {@link ApiException} on failure.
     */
    public AuthenticatedUser login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw ApiException.unauthorized(GENERIC_LOGIN_ERROR);
        }
        try (Connection conn = Database.getDataSource().getConnection()) {
            Optional<User> userOpt = userDao.findByUsername(conn, username);
            if (userOpt.isEmpty()) {
                throw ApiException.unauthorized(GENERIC_LOGIN_ERROR);
            }
            User user = userOpt.get();

            if ("INACTIVE".equals(user.getStatus()) || "SUSPENDED".equals(user.getStatus())) {
                throw ApiException.forbidden("This account is not active.");
            }
            if (isLocked(user)) {
                throw ApiException.badRequest("ACCOUNT_LOCKED", "Account locked. Try again later.");
            }

            if (!PasswordHasher.verify(password, user.getPasswordHash())) {
                failLogin(conn, user);
                throw ApiException.unauthorized(GENERIC_LOGIN_ERROR);
            }

            userDao.registerLoginSuccess(conn, user.getUserId());
            return buildPrincipal(conn, user);
        } catch (SQLException e) {
            throw ApiException.internal("Login could not be completed.");
        }
    }

    private void failLogin(Connection conn, User user) throws SQLException {
        int failed = user.getFailedLoginCount() + 1;
        Timestamp lockedUntil = null;
        if (failed >= MAX_FAILED_LOGINS) {
            lockedUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            userDao.registerFailedLogin(conn, user.getUserId(), failed, lockedUntil);
            return;
        }
        userDao.registerFailedLogin(conn, user.getUserId(), failed, null);
    }

    private static boolean isLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }
        return user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private AuthenticatedUser buildPrincipal(Connection conn, User user) throws SQLException {
        List<Role> roles = userDao.findRoles(conn, user.getUserId());
        List<Permission> permissions = userDao.findPermissions(conn, user.getUserId());
        List<BusinessUnit> businessUnits = businessUnitDao.listAssignedToUser(conn, user.getOrganizationId(), user.getUserId());
        boolean mustChangePassword = user.getPasswordChangedAt() == null;
        return new AuthenticatedUser(user.getUserId(), user.getOrganizationId(), user.getUsername(),
                user.getFirstName(), user.getLastName(), user.getEmail(), user.getStatus(),
                roles, permissions, businessUnits, mustChangePassword);
    }

    /**
     * Changes the current user's password after verifying the existing one.
     *
     * @return true when the caller must re-establish their session state
     */
    public void changePassword(long userId, String currentPassword, String newPassword) {
        validateNewPassword(newPassword);
        try (Connection conn = Database.getDataSource().getConnection()) {
            Optional<User> userOpt = userDao.findById(conn, userId);
            if (userOpt.isEmpty()) {
                throw ApiException.notFound("USER", "User not found.");
            }
            User user = userOpt.get();
            if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
                throw ApiException.validation("CURRENT_PASSWORD", "Current password is incorrect.");
            }
            String hash = PasswordHasher.hash(newPassword);
            userDao.updatePassword(conn, userId, hash);
            conn.commit();
        } catch (SQLException e) {
            throw ApiException.internal("Password change could not be completed.");
        }
    }

    public static void validateNewPassword(String password) {
        if (password == null || password.length() < 10) {
            throw ApiException.validation("NEW_PASSWORD", "Password must be at least 10 characters.");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!(hasUpper && hasLower && hasDigit)) {
            throw ApiException.validation("NEW_PASSWORD",
                    "Password must contain upper case, lower case and a digit.");
        }
    }

    /** Reloads a principal fresh from the database (session-refresh helper). */
    public AuthenticatedUser refresh(long userId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            Optional<User> userOpt = userDao.findById(conn, userId);
            if (userOpt.isEmpty()) {
                throw ApiException.unauthorized(GENERIC_LOGIN_ERROR);
            }
            return buildPrincipal(conn, userOpt.get());
        } catch (SQLException e) {
            throw ApiException.internal("Unable to load account.");
        }
    }
}