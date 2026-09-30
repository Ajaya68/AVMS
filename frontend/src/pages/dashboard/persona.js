/**
 * Maps the signed-in user to a dashboard persona.
 *
 * Priority: superuser / ADMIN first, then the most specific staff role.
 * Users with custom permission mixes fall back by permission breadth so a
 * dashboard never renders empty when the user can actually see modules.
 */
export function detectPersona(user) {
  if (!user) return "employee";
  if (user.is_superuser) return "admin";
  const codes = new Set(user.role_codes || []);
  if (codes.has("ADMIN")) return "admin";
  if (codes.has("MANAGER")) return "manager";
  if (codes.has("ACCOUNTANT")) return "accountant";
  if (codes.has("SALES_STAFF")) return "sales";
  if (codes.has("INVENTORY_STAFF")) return "inventory";
  if (codes.has("EMPLOYEE")) return "employee";
  const perms = new Set(user.permissions || []);
  if (perms.has("users.manage") || perms.has("audit.view")) return "admin";
  if (
    perms.has("sales.view") ||
    perms.has("purchases.view") ||
    perms.has("inventory.view") ||
    perms.has("reports.view")
  ) {
    return "manager";
  }
  return "employee";
}

export const PERSONA_META = {
  admin: {
    title: "Dashboard",
    subtitle: "Full control — every module, users and system status",
  },
  manager: {
    title: "Operations Dashboard",
    subtitle: "Sales, purchases, stock and team at a glance",
  },
  accountant: {
    title: "Finance Dashboard",
    subtitle: "Revenue, expenses, dues and cash flow",
  },
  sales: {
    title: "Sales Dashboard",
    subtitle: "Your bills, customers and collections",
  },
  inventory: {
    title: "Inventory Dashboard",
    subtitle: "Stock levels, movements and warehouses",
  },
  employee: {
    title: "My Dashboard",
    subtitle: "Your profile and attendance",
  },
};
