import api from "./api";

export async function fetchSales(params = {}) {
  return api.get("/sales/", { params });
}

export async function fetchSale(id) {
  return api.get(`/sales/${id}/`);
}

export async function fetchSaleItems(id) {
  return api.get(`/sales/${id}/items/`);
}

export async function createSale(payload) {
  return api.post("/sales/", payload);
}

export async function updateSale(id, payload) {
  return api.patch(`/sales/${id}/`, payload);
}

export async function deleteSale(id) {
  return api.delete(`/sales/${id}/`);
}

export async function fetchSaleReturns(params = {}) {
  return api.get("/sales-returns/", { params });
}

export async function createSaleReturn(payload) {
  return api.post("/sales-returns/", payload);
}

export const SALE_STATUSES = [
  "PENDING",
  "PARTIAL",
  "COMPLETED",
  "RETURNED",
  "CANCELLED",
];

export function saleStatusLabel(status) {
  return (status || "")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export default {
  fetchSales,
  fetchSale,
  fetchSaleItems,
  createSale,
  updateSale,
  deleteSale,
  fetchSaleReturns,
  createSaleReturn,
  SALE_STATUSES,
  saleStatusLabel,
};