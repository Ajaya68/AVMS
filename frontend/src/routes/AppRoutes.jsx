import { createBrowserRouter, Route, createRoutesFromElements } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import Dashboard from "../pages/dashboard/Dashboard";
import Login from "../pages/auth/Login";
import ForgotPassword from "../pages/auth/ForgotPassword";
import ResetPassword from "../pages/auth/ResetPassword";
import NotFound from "../pages/NotFound";
import UsersPage from "../pages/users/UsersPage";
import RolesPage from "../pages/roles/RolesPage";
import VenturesPage from "../pages/ventures/VenturesPage";
import VentureDetailPage from "../pages/ventures/VentureDetailPage";
import CustomersPage from "../pages/customers/CustomersPage";
import SuppliersPage from "../pages/suppliers/SuppliersPage";
import ProductsPage from "../pages/products/ProductsPage";
import CategoriesPage from "../pages/products/CategoriesPage";
import UnitsPage from "../pages/products/UnitsPage";
import WarehousesPage from "../pages/inventory/WarehousesPage";
import InventoryPage from "../pages/inventory/InventoryPage";
import StockMovementsPage from "../pages/inventory/StockMovementsPage";
import PurchasesPage from "../pages/purchases/PurchasesPage";
import PurchaseReturnsPage from "../pages/purchases/PurchaseReturnsPage";
import SalesPage from "../pages/sales/SalesPage";
import SaleReturnsPage from "../pages/sales/SaleReturnsPage";
import PaymentsPage from "../pages/payments/PaymentsPage";
import ExpensesPage from "../pages/expenses/ExpensesPage";
import EmployeesPage from "../pages/employees/EmployeesPage";
import AttendancePage from "../pages/attendance/AttendancePage";
import ReportsPage from "../pages/reports/ReportsPage";
import NotificationsPage from "../pages/notifications/NotificationsPage";
import AuditLogsPage from "../pages/audit/AuditLogsPage";
import SettingsPage from "../pages/settings/SettingsPage";
import VentureRemount from "../components/VentureRemount";

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route
        element={
          <ProtectedRoute>
            <VentureRemount />
          </ProtectedRoute>
        }
      >
      <Route index element={<Dashboard />} />
      <Route path="ventures" element={<VenturesPage />} />
      <Route path="ventures/:id" element={<VentureDetailPage />} />
      <Route path="customers" element={<CustomersPage />} />
      <Route path="suppliers" element={<SuppliersPage />} />
      <Route path="products" element={<ProductsPage />} />
      <Route path="categories" element={<CategoriesPage />} />
      <Route path="units" element={<UnitsPage />} />
      <Route path="warehouses" element={<WarehousesPage />} />
      <Route path="inventory" element={<InventoryPage />} />
      <Route path="stock-movements" element={<StockMovementsPage />} />
      <Route path="sales" element={<SalesPage />} />
      <Route path="purchases" element={<PurchasesPage />} />
      <Route path="sales-returns" element={<SaleReturnsPage />} />
      <Route path="purchase-returns" element={<PurchaseReturnsPage />} />
      <Route path="payments" element={<PaymentsPage />} />
      <Route path="expenses" element={<ExpensesPage />} />
      <Route path="employees" element={<EmployeesPage />} />
      <Route path="attendance" element={<AttendancePage />} />
      <Route path="reports" element={<ReportsPage />} />
      <Route path="users" element={<UsersPage />} />
      <Route path="roles" element={<RolesPage />} />
      <Route path="notifications" element={<NotificationsPage />} />
      <Route path="audit-logs" element={<AuditLogsPage />} />
      <Route path="settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<NotFound />} />
    </>
  )
);

export { router };
export default router;