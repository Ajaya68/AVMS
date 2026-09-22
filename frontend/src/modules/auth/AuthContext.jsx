import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api, { setActiveBusinessUnit } from '../../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [permissions, setPermissions] = useState([]);
  const [businessUnits, setBusinessUnits] = useState([]);
  const [activeBusinessUnitId, setActiveBusinessUnitId] = useState(null);
  const [loading, setLoading] = useState(true);

  const applySession = useCallback((units) => {
    const list = units || [];
    setBusinessUnits(list);
    const preferred = list[0] ? list[0].business_unit_id : null;
    setActiveBusinessUnitId((current) => {
      const next = current != null ? current : preferred;
      setActiveBusinessUnit(next);
      return next;
    });
  }, []);

  // Restore session on mount
  useEffect(() => {
    (async () => {
      try {
        const data = await api.get('/api/v1/auth/me');
        setUser(data.user);
        setPermissions(data.permissions || []);
        applySession(data.businessUnits);
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    })();
  }, [applySession]);

  const login = useCallback(async (username, password) => {
    const data = await api.post('/api/v1/auth/login', { username, password });
    setUser(data.user);
    setPermissions(data.permissions || []);
    applySession(data.businessUnits);
    return data;
  }, [applySession]);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/v1/auth/logout');
    } finally {
      setUser(null);
      setPermissions([]);
      setBusinessUnits([]);
      setActiveBusinessUnitId(null);
      setActiveBusinessUnit(null);
    }
  }, []);

  const selectBusinessUnit = useCallback((id) => {
    setActiveBusinessUnitId(id);
    setActiveBusinessUnit(id);
  }, []);

  const hasPermission = useCallback((code) => permissions.includes(code), [permissions]);

  const value = {
    user, permissions, businessUnits, activeBusinessUnitId, loading,
    login, logout, selectBusinessUnit, hasPermission,
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}