import api from "./api";

export async function fetchPurchases(params = {}) {
  return api.get("/purchases/", { params });
}

export async function fetchPurchase(id) {
  return api.get(`/purchases/${id}/`);
}

export async function fetchPurchaseItems(id) {
  return api.get(`/purchases/${id}/items/`);
}

export async function createPurchase(payload) {
  return api.post("/purchases/", payload);
}

export async function updatePurchase(id, payload) {
  return api.patch(`/purchases/${id}/`, payload);
}

export async function deletePurchase(id) {
  return api.delete(`/purchases/${id}/`);
}

export async function fetchPurchaseReturns(params = {}) {
  return api.get("/purchase-returns/", { params });
}

export async function createPurchaseReturn(payload) {
  return api.post("/purchase-returns/", payload);
}

export const PURCHASE_STATUSES = [
  "PENDING",
  "PARTIAL",
  "COMPLETED",
  "RETURNED",
  "CANCELLED",
];

export function purchaseStatusLabel(status) {
  return (status || "")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export default {
  fetchPurchases,
  fetchPurchase,
  fetchPurchaseItems,
  createPurchase,
  updatePurchase,
  deletePurchase,
  fetchPurchaseReturns,
  createPurchaseReturn,
  PURCHASE_STATUSES,
  purchaseStatusLabel,
};