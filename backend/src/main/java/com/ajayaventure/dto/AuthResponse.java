package com.ajayaventure.dto;

import com.ajayaventure.model.Role;
import com.ajayaventure.security.AuthenticatedUser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Authenticated-session response. Contains only safe projections; never the
 * password hash.
 */
public class AuthResponse {

    private Map<String, Object> user;
    private List<String> roles;
    private List<String> permissions;
    private List<BusinessUnitAccess> businessUnits;
    private boolean mustChangePassword;

    public static AuthResponse from(AuthenticatedUser principal, List<BusinessUnitAccess> businessUnits) {
        AuthResponse out = new AuthResponse();
        out.user = Map.of(
                "user_id", principal.getUserId(),
                "organization_id", principal.getOrganizationId(),
                "username", principal.getUsername(),
                "first_name", nullToEmpty(principal.getFirstName()),
                "last_name", nullToEmpty(principal.getLastName()),
                "email", principal.getEmail(),
                "status", principal.getStatus());
        out.roles = principal.getRoles().stream().map(Role::getRoleCode).toList();
        out.permissions = principal.getPermissions().stream().sorted().toList();
        out.businessUnits = businessUnits;
        out.mustChangePassword = principal.isMustChangePassword();
        return out;
    }

    public static AuthResponse from(AuthenticatedUser principal) {
        List<BusinessUnitAccess> accesses = principal.getBusinessUnits().stream()
                .map(bu -> new BusinessUnitAccess(bu.getBusinessUnitId(), bu.getBusinessUnitCode(),
                        bu.getBusinessUnitName(), bu.getBusinessUnitType(), "FULL"))
                .collect(Collectors.toList());
        return from(principal, accesses);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<BusinessUnitAccess> getBusinessUnits() {
        return businessUnits;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}