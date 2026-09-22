package com.ajayaventure.service;

import com.ajayaventure.config.Database;
import com.ajayaventure.dao.UserDao;
import com.ajayaventure.dto.BusinessUnitAccess;
import com.ajayaventure.dto.CreateUserRequest;
import com.ajayaventure.exception.ApiException;
import com.ajayaventure.exception.FieldError;
import com.ajayaventure.model.User;
import com.ajayaventure.security.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserDao userDao = new UserDao();

    public User create(long organizationId, Long actorUserId, CreateUserRequest request) {
        FieldError.Holder errors = new FieldError.Holder();
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            errors.add("username", "Username is required.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            errors.add("email", "Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 10) {
            errors.add("password", "Password must be at least 10 characters.");
        }
        if (request.getRoleId() == null) {
            errors.add("role_id", "Role is required.");
        }
        if (request.getBusinessUnitIds() == null || request.getBusinessUnitIds().isEmpty()) {
            errors.add("business_unit_ids", "At least one business unit is required.");
        }
        errors.throwIfNotEmpty();

        String accessLevel = request.getAccessLevel() == null || request.getAccessLevel().isBlank()
                ? "FULL" : request.getAccessLevel();
        if (!List.of("FULL", "VIEW").contains(accessLevel)) {
            throw ApiException.badRequest("ACCESS_LEVEL", "Invalid access level.");
        }

        String status = request.getStatus() == null ? "ACTIVE" : request.getStatus();
        if (!List.of("ACTIVE", "INACTIVE", "SUSPENDED").contains(status)) {
            throw ApiException.badRequest("STATUS", "Invalid status.");
        }

        try (Connection conn = Database.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (userDao.findByUsername(conn, request.getUsername()).isPresent()) {
                    throw ApiException.conflict("USERNAME", "Username already exists.");
                }
                if (userDao.findByEmail(conn, request.getEmail()).isPresent()) {
                    throw ApiException.conflict("EMAIL", "Email already exists.");
                }

                User user = new User();
                user.setOrganizationId(organizationId);
                user.setUsername(request.getUsername());
                user.setEmail(request.getEmail());
                user.setMobile(request.getMobile());
                String firstName = request.getFirstName();
                String lastName = request.getLastName();
                if ((firstName == null || firstName.isBlank())
                        && request.getFullName() != null && !request.getFullName().isBlank()) {
                    String[] parts = request.getFullName().trim().split("\\s+", 2);
                    firstName = parts[0];
                    lastName = parts.length > 1 ? parts[1] : null;
                }
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setStatus(status);
                user.setPasswordHash(PasswordHasher.hash(request.getPassword()));

                long userId = userDao.insert(conn, user);
                user.setUserId(userId);

                userDao.insertUserRole(conn, userId, request.getRoleId());
                for (Long buId : request.getBusinessUnitIds()) {
                    userDao.insertUserBusinessUnit(conn, userId, organizationId, buId, accessLevel, actorUserId);
                }
                conn.commit();
                return user;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ApiException api) {
                    throw api;
                }
                throw e;
            }
        } catch (SQLException e) {
            throw ApiException.internal("User could not be created.");
        }
    }

    public List<User> list(long organizationId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        try (Connection conn = Database.getDataSource().getConnection()) {
            return userDao.list(conn, organizationId, (safePage - 1) * safeSize, safeSize);
        } catch (SQLException e) {
            throw ApiException.internal("Users could not be loaded.");
        }
    }

    public long count(long organizationId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return userDao.count(conn, organizationId);
        } catch (SQLException e) {
            throw ApiException.internal("User count could not be loaded.");
        }
    }

    public Optional<User> findById(long userId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return userDao.findById(conn, userId);
        } catch (SQLException e) {
            throw ApiException.internal("User could not be loaded.");
        }
    }

    public List<BusinessUnitAccess> businessUnitAccess(long organizationId, long userId) {
        try (Connection conn = Database.getDataSource().getConnection()) {
            return userDao.listBusinessUnitAccess(conn, organizationId, userId);
        } catch (SQLException e) {
            throw ApiException.internal("User business-unit access could not be loaded.");
        }
    }
}