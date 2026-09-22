package com.ajayaventure.security;

import com.ajayaventure.model.BusinessUnit;
import com.ajayaventure.model.Permission;
import com.ajayaventure.model.Role;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Server-side authentication principal. Stored in the HTTP session after
 * login and attached to each request by {@link AuthenticationFilter}. All
 * authorization decisions read from here, never from the client.
 */
public class AuthenticatedUser {

    private final long userId;
    private final long organizationId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String status;
    private final List<Role> roles;
    private final Set<String> permissions;
    private final List<BusinessUnit> businessUnits;
    private final boolean mustChangePassword;

    public AuthenticatedUser(long userId, long organizationId, String username, String firstName,
                             String lastName, String email, String status, List<Role> roles,
                             List<Permission> permissions, List<BusinessUnit> businessUnits,
                             boolean mustChangePassword) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
        this.roles = roles;
        this.permissions = permissions.stream().map(Permission::getPermissionCode).collect(Collectors.toSet());
        this.businessUnits = businessUnits;
        this.mustChangePassword = mustChangePassword;
    }

    public long getUserId() {
        return userId;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public List<BusinessUnit> getBusinessUnits() {
        return businessUnits;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /** True when the principal has access to the given business unit. */
    public boolean hasBusinessUnit(long businessUnitId) {
        return businessUnits.stream().anyMatch(bu -> bu.getBusinessUnitId() == businessUnitId);
    }
}