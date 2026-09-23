import { createContext, useContext, useState } from "react";

/**
 * Authentication state container.
 *
 * Phase 1 provides the shell only. The login/logout flows and JWT session
 * management are implemented in Phase 2. Components may consume the shape
 * below; until Phase 2 a user is always unauthenticated.
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);

  const value = {
    user,
    loading: false,
    isAuthenticated: Boolean(user),
    login: async () => {
      // Phase 2: call the auth API and set the authenticated user.
    },
    logout: () => {
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
      setUser(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext);
}