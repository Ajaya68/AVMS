/**
 * Sidebar navigation definition.
 *
 * Every module declares the capability code required to see it. The sidebar
 * filters items by the signed-in user's permissions; the backend enforces
 * the same rules on the corresponding endpoints.
 */
const NAV = [
  {
    section: null,
    items: [
      { label: "Dashboard", path: "/", icon: "bi-speedometer2", perm: "dashboard.view" },
    ],
  },
  {
    section: "Business",
    items: [
      { label: "Ventures", path: "/ventures", icon: "bi-building", perm: "ventures.view" },
    ],
  },
  {
    section: "Masters",
    items: [
      { label: "Customers", path: "/customers", icon: "bi-people", perm: "customers.view" },
      { label: "Suppliers", path: "/suppliers", icon: "bi-truck", perm: "suppliers.view" },
      { label: "Products", path: "/products", icon: "bi-box-seam", perm: "products.view" },
      { label: "Categories", path: "/categories", icon: "bi-tags", perm: "categories.view" },
      { label: "Units", path: "/units", icon: "bi-rulers", perm: "units.view" },
    ],
  },
  {
    section: "Inventory",
    items: [
      { label: "Warehouses", path: "/warehouses", icon: "bi-archive", perm: "warehouses.view" },
      { label: "Inventory", path: "/inventory", icon: "bi-clipboard-data", perm: "inventory.view" },
      { label: "Stock Movements", path: "/stock-movements", icon: "bi-arrow-left-right", perm: "stock_movements.view" },
    ],
  },
  {
    section: "Transactions",
    items: [
      { label: "Sales", path: "/sales", icon: "bi-cart-check", perm: "sales.view" },
      { label: "Purchases", path: "/purchases", icon: "bi-cart-plus", perm: "purchases.view" },
      { label: "Sales Returns", path: "/sales-returns", icon: "bi-arrow-counterclockwise", perm: "sales_returns.view" },
      { label: "Purchase Returns", path: "/purchase-returns", icon: "bi-arrow-counterclockwise", perm: "purchase_returns.view" },
      { label: "Payments", path: "/payments", icon: "bi-credit-card", perm: "payments.view" },
      { label: "Expenses", path: "/expenses", icon: "bi-wallet2", perm: "expenses.view" },
    ],
  },
  {
    section: "Management",
    items: [
      { label: "Employees", path: "/employees", icon: "bi-person-badge", perm: "employees.view" },
      { label: "Reports", path: "/reports", icon: "bi-graph-up", perm: "reports.view" },
    ],
  },
  {
    section: "System",
    items: [
      { label: "Users", path: "/users", icon: "bi-person-gear", perm: "users.view" },
      { label: "Roles", path: "/roles", icon: "bi-shield-lock", perm: "roles.view" },
      { label: "Notifications", path: "/notifications", icon: "bi-bell", perm: "notifications.view" },
      { label: "Audit Logs", path: "/audit-logs", icon: "bi-journal-text", perm: "audit.view" },
      { label: "Settings", path: "/settings", icon: "bi-gear", perm: "settings.view" },
    ],
  },
];

export default NAV;