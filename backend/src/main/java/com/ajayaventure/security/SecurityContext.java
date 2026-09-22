package com.ajayaventure.security;

import com.ajayaventure.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads the current authenticated principal and enforces permission /
 * business-unit scope checks server-side. Permissions and scopes are never
 * taken from request parameters or the frontend.
 */
public final class SecurityContext {

    public static final String ATTR_PRINCIPAL = "av.authenticated";
    public static final String ATTR_BUSINESS_UNIT = "resolved.businessUnitId";

    private SecurityContext() {
    }

    public static AuthenticatedUser requireUser(HttpServletRequest request) {
        Object principal = request.getAttribute(ATTR_PRINCIPAL);
        if (!(principal instanceof AuthenticatedUser user)) {
            throw ApiException.unauthorized("Authentication required.");
        }
        return user;
    }

    public static AuthenticatedUser currentUser(HttpServletRequest request) {
        Object principal = request.getAttribute(ATTR_PRINCIPAL);
        return principal instanceof AuthenticatedUser user ? user : null;
    }

    /**
     * Resolves the effective business-unit scope for a request.
     *
     * <p>The client may pass {@code X-Business-Unit-Id}. The server verifies the
     * current user's assignment before the value is used anywhere; a malicious
     * caller cannot widen scope by editing the header. Absent header means
     * organization-wide/all-business context.</p>
     */
    public static Long resolveBusinessUnit(HttpServletRequest request) {
        AuthenticatedUser user = requireUser(request);
        String header = request.getHeader("X-Business-Unit-Id");
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            long businessUnitId = Long.parseLong(header);
            if (!user.hasBusinessUnit(businessUnitId)) {
                throw ApiException.forbidden("No access to the requested business unit.");
            }
            request.setAttribute(ATTR_BUSINESS_UNIT, businessUnitId);
            return businessUnitId;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("BUSINESS_UNIT", "Invalid business unit id.");
        }
    }

    /** Returns the already-resolved business unit or null when unscoped. */
    public static Long currentBusinessUnit(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_BUSINESS_UNIT);
        return value instanceof Long id ? id : null;
    }

    public static void requirePermission(HttpServletRequest request, String permissionCode) {
        AuthenticatedUser user = requireUser(request);
        if (!user.getPermissions().contains(permissionCode)) {
            throw ApiException.forbidden("Insufficient permissions (required: " + permissionCode + ").");
        }
    }

    public static boolean hasPermission(HttpServletRequest request, String permissionCode) {
        AuthenticatedUser user = currentUser(request);
        return user != null && user.getPermissions().contains(permissionCode);
    }
}