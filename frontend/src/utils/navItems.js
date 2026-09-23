/**
 * Sidebar navigation definition.
 *
 * Modules are progressively enabled as their backend + frontend are
 * completed. Until then each entry resolves to a placeholder page.
 */
const NAV = [
  {
    section: null,
    items: [{ label: "Dashboard", path: "/", icon: "bi-speedometer2" }],
  },
  {
    section: "Business",
    items: [{ label: "Ventures", path: "/ventures", icon: "bi-building" }],
  },
  {
    section: "Masters",
    items: [
      { label: "Customers", path: "/customers", icon: "bi-people" },
      { label: "Suppliers", path: "/suppliers", icon: "bi-truck" },
      { label: "Products", path: "/products", icon: "bi-box-seam" },
      { label: "Categories", path: "/categories", icon: "bi-tags" },
      { label: "Units", path: "/units", icon: "bi-rulers" },
    ],
  },
  {
    section: "Inventory",
    items: [
      { label: "Warehouses", path: "/warehouses", icon: "bi-archive" },
      { label: "Inventory", path: "/inventory", icon: "bi-clipboard-data" },
      { label: "Stock Movements", path: "/stock-movements", icon: "bi-arrow-left-right" },
    ],
  },
  {
    section: "Transactions",
    items: [
      { label: "Sales", path: "/sales", icon: "bi-cart-check" },
      { label: "Purchases", path: "/purchases", icon: "bi-cart-plus" },
      { label: "Sales Returns", path: "/sales-returns", icon: "bi-arrow-counterclockwise" },
      { label: "Purchase Returns", path: "/purchase-returns", icon: "bi-arrow-counterclockwise" },
      { label: "Payments", path: "/payments", icon: "bi-credit-card" },
      { label: "Expenses", path: "/expenses", icon: "bi-wallet2" },
    ],
  },
  {
    section: "Management",
    items: [
      { label: "Employees", path: "/employees", icon: "bi-person-badge" },
      { label: "Reports", path: "/reports", icon: "bi-graph-up" },
    ],
  },
  {
    section: "System",
    items: [
      { label: "Users", path: "/users", icon: "bi-person-gear" },
      { label: "Roles", path: "/roles", icon: "bi-shield-lock" },
      { label: "Notifications", path: "/notifications", icon: "bi-bell" },
      { label: "Audit Logs", path: "/audit-logs", icon: "bi-journal-text" },
      { label: "Settings", path: "/settings", icon: "bi-gear" },
    ],
  },
];

export default NAV;