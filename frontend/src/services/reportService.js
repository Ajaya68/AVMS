import api from "./api";

export const fetchSalesReport = (params = {}) =>
  api.get("/reports/sales/", { params });

export const fetchPurchasesReport = (params = {}) =>
  api.get("/reports/purchases/", { params });

export const fetchInventoryReport = (params = {}) =>
  api.get("/reports/inventory/", { params });

export const fetchFinancialReport = (params = {}) =>
  api.get("/reports/financial/", { params });