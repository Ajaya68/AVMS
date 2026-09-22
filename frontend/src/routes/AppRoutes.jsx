import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import Dashboard from '../pages/Dashboard';
import Login from '../modules/auth/Login';
import NotFound from '../pages/NotFound';
import OrganizationPage from '../modules/organization/OrganizationPage';
import UsersPage from '../modules/users/UsersPage';
import BusinessUnitsPage from '../modules/businessunits/BusinessUnitsPage';
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

function Guarded({ permission, children }) {
  const { hasPermission } = useAuth();
  if (!hasPermission(permission)) {
    return <Navigate to="/" replace />;
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
        <Route
          path="organization"
          element={
            <Guarded permission="ORG.VIEW">
              <OrganizationPage />
            </Guarded>
          }
        />
        <Route
          path="users"
          element={
            <Guarded permission="USER.VIEW">
              <UsersPage />
            </Guarded>
          }
        />
        <Route
          path="business-units"
          element={
            <Guarded permission="BU.VIEW">
              <BusinessUnitsPage />
            </Guarded>
          }
        />
        <Route path="*" element={<NotFound />} />
      </Route>
      {!user && <Route path="/*" element={<Navigate to="/login" replace />} />}
    </Routes>
  );
}

export { AuthProvider };