import api from "./api";

export async function fetchWarehouses(params = {}) {
  return api.get("/warehouses/", { params });
}

export async function createWarehouse(payload) {
  return api.post("/warehouses/", payload);
}

export async function updateWarehouse(id, payload) {
  return api.patch(`/warehouses/${id}/`, payload);
}

export async function deleteWarehouse(id) {
  return api.delete(`/warehouses/${id}/`);
}

export async function fetchInventory(params = {}) {
  return api.get("/inventory/", { params });
}

export async function fetchStockMovements(params = {}) {
  return api.get("/stock-movements/", { params });
}

export async function createStockMovement(payload) {
  return api.post("/stock-movements/", payload);
}

export const MOVEMENT_TYPES = [
  "PURCHASE",
  "SALE",
  "PURCHASE_RETURN",
  "SALES_RETURN",
  "ADJUSTMENT_IN",
  "ADJUSTMENT_OUT",
  "TRANSFER_OUT",
];

export function movementApiLabel(type) {
  return type.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

export default {
  fetchWarehouses,
  createWarehouse,
  updateWarehouse,
  deleteWarehouse,
  fetchInventory,
  fetchStockMovements,
  createStockMovement,
  MOVEMENT_TYPES,
  movementApiLabel,
};