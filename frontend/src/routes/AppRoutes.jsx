import { createBrowserRouter, Route, createRoutesFromElements } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import ProtectedRoute from "../components/ProtectedRoute";
import Dashboard from "../pages/dashboard/Dashboard";
import Login from "../pages/auth/Login";
import NotFound from "../pages/NotFound";
import PlaceholderPage from "../pages/PlaceholderPage";
import UsersPage from "../pages/users/UsersPage";
import RolesPage from "../pages/roles/RolesPage";
import VenturesPage from "../pages/ventures/VenturesPage";
import VentureDetailPage from "../pages/ventures/VentureDetailPage";

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }
      >
      <Route index element={<Dashboard />} />
      <Route path="ventures" element={<VenturesPage />} />
      <Route path="ventures/:id" element={<VentureDetailPage />} />
      <Route path="customers" element={<PlaceholderPage />} />
      <Route path="suppliers" element={<PlaceholderPage />} />
      <Route path="products" element={<PlaceholderPage />} />
      <Route path="categories" element={<PlaceholderPage />} />
      <Route path="units" element={<PlaceholderPage />} />
      <Route path="warehouses" element={<PlaceholderPage />} />
      <Route path="inventory" element={<PlaceholderPage />} />
      <Route path="stock-movements" element={<PlaceholderPage />} />
      <Route path="sales" element={<PlaceholderPage />} />
      <Route path="purchases" element={<PlaceholderPage />} />
      <Route path="sales-returns" element={<PlaceholderPage />} />
      <Route path="purchase-returns" element={<PlaceholderPage />} />
      <Route path="payments" element={<PlaceholderPage />} />
      <Route path="expenses" element={<PlaceholderPage />} />
      <Route path="employees" element={<PlaceholderPage />} />
      <Route path="reports" element={<PlaceholderPage />} />
      <Route path="users" element={<UsersPage />} />
      <Route path="roles" element={<RolesPage />} />
      <Route path="notifications" element={<PlaceholderPage />} />
      <Route path="audit-logs" element={<PlaceholderPage />} />
      <Route path="settings" element={<PlaceholderPage />} />
      </Route>
      <Route path="*" element={<NotFound />} />
    </>
  )
);

export { router };
export default router;