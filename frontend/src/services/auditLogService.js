import api from "./api";

export const fetchAuditLogs = (params = {}) =>
  api.get("/audit-logs/", { params: { page_size: 25, ...params } });

export const AUDIT_ACTIONS = [
  "LOGIN",
  "LOGOUT",
  "CREATE",
  "UPDATE",
  "DELETE",
  "SALE_CREATED",
  "PURCHASE_CREATED",
  "PAYMENT_CREATED",
  "STOCK_ADJUSTED",
  "USER_CREATED",
];