import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import Dashboard from '../pages/Dashboard';
import Login from '../modules/auth/Login';
import NotFound from '../pages/NotFound';
import { AuthProvider, useAuth } from '../modules/auth/AuthContext';

function Protected({ children }) {
  const { user, loading } = useAuth();
  if (loading) {
    return <div className="d-flex justify-content-center p-5">Loading…</div>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function AppRoutes() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <Protected>
            <MainLayout />
          </Protected>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="*" element={<NotFound />} />
      </Route>
      {!user && <Route path="/*" element={<Navigate to="/login" replace />} />}
    </Routes>
  );
}

export { AuthProvider };