import { useAuth } from "../../context/AuthContext";
import { detectPersona } from "./persona";
import AccountantDashboard from "./AccountantDashboard";
import AdminDashboard from "./AdminDashboard";
import EmployeeDashboard from "./EmployeeDashboard";
import InventoryDashboard from "./InventoryDashboard";
import ManagerDashboard from "./ManagerDashboard";
import SalesDashboard from "./SalesDashboard";

/**
 * Persona router: every signed-in user lands on the dashboard that
 * matches their role — admins get full control, managers get
 * operations, accountants get finance, sales/inventory staff get
 * their modules, and employees get an attendance-first view.
 */
function Dashboard() {
  const { user } = useAuth();
  switch (detectPersona(user)) {
    case "manager":
      return <ManagerDashboard />;
    case "accountant":
      return <AccountantDashboard />;
    case "sales":
      return <SalesDashboard />;
    case "inventory":
      return <InventoryDashboard />;
    case "employee":
      return <EmployeeDashboard />;
    case "admin":
    default:
      return <AdminDashboard />;
  }
}

export default Dashboard;
