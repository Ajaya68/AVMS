import api from "./api";

export const fetchSalesReport = (params = {}) =>
  api.get("/reports/sales/", { params }).then((r) => r.data.data);

export const fetchPurchasesReport = (params = {}) =>
  api.get("/reports/purchases/", { params }).then((r) => r.data.data);

export const fetchInventoryReport = (params = {}) =>
  api.get("/reports/inventory/", { params }).then((r) => r.data.data);

export const fetchFinancialReport = (params = {}) =>
  api.get("/reports/financial/", { params }).then((r) => r.data.data);