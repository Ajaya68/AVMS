import { createContext, useContext, useCallback, useMemo, useState } from 'react';

const AuthContext = createContext(null);

// Phase 1 wires this to /api/v1/auth/*. The context deliberately holds only the
// minimal, server-approved identity: the backend is authoritative for access.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  const login = useCallback(async (authedUser) => {
    setUser(authedUser);
  }, []);

  const logout = useCallback(async () => {
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}