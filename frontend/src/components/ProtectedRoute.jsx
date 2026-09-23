import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import LoadingSpinner from "./LoadingSpinner";

/**
 * Client-side route guard. The backend independently verifies the JWT and
 * role permissions - this guard is only a UX convenience.
 *
 * Guarded behind a feature flag: authentication arrives in Phase 2, until
 * then the shell is freely navigable so that foundation work can be
 * verified end to end.
 */
const AUTH_ENABLED = false;

function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (!AUTH_ENABLED) {
    return children;
  }

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}

export default ProtectedRoute;