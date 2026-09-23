import { createContext, useContext, useEffect, useMemo, useState } from "react";

import * as authService from "../services/authService";

/**
 * Authentication state container.
 *
 * On first mount the provider hydrates the session from the stored JWT via
 * /auth/me/. The view layer gates routes and navigation with the auth state
 * and the user's granted capability codes.
 */
const AuthContext = createContext(null);

const AUTH_ENABLED = true;

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [checked, setChecked] = useState(!AUTH_ENABLED);

  useEffect(() => {
    if (!AUTH_ENABLED) {
      return;
    }
    let cancelled = false;
    Promise.resolve()
      .then(() => authService.getAccessToken())
      .then((access) => (access ? authService.fetchMe() : null))
      .then((me) => {
        if (!cancelled) setUser(me);
      })
      .catch(() => {
        if (!cancelled) authService.logout();
      })
      .finally(() => {
        if (!cancelled) setChecked(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo(() => {
    const hasPerm = (code) => {
      if (!AUTH_ENABLED) return true;
      if (!user) return false;
      if (user.is_superuser) return true;
      return Array.isArray(user.permissions) && user.permissions.includes(code);
    };
    const loading = !checked;
    return {
      user,
      loading,
      isAuthenticated: !AUTH_ENABLED || Boolean(user),
      hasPerm,
      login: async (email, password) => {
        const loggedInUser = await authService.login(email, password);
        setUser(loggedInUser);
        setChecked(true);
        return loggedInUser;
      },
      logout: async () => {
        await authService.logout();
        setUser(null);
        setChecked(true);
      },
    };
  }, [user, checked]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext);
}